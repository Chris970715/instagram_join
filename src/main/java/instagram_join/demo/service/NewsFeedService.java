package instagram_join.demo.service;

import instagram_join.demo.dao.FollowerDAO;
import instagram_join.demo.dao.PostDAO;
import instagram_join.demo.dto.PostDTO;
import instagram_join.demo.dto.UserDTO;
import instagram_join.demo.entity.Follower;
import instagram_join.demo.entity.Post;
import instagram_join.demo.entity.User;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j // 로깅을 위한 Lombok 어노테이션. 이 클래스를 통해 로그 메시지를 기록할 수 있음
@Service // 스프링 서비스 레이어를 나타내는 어노테이션
@CacheConfig(cacheNames = "newsFeedCache") // 캐시 설정을 위한 어노테이션. 기본 캐시 이름을 지정
public class NewsFeedService {

    private static final String NEWS_FEED_KEY_PREFIX = "newsfeed:";
    // - Redis에서 각 사용자의 뉴스피드를 저장할 때 사용하는 키의 접두사

    private static final String FANOUT_STREAM_KEY = "fanout:stream";
    // - Redis Stream의 고유 식별자로 사용
    // - 모든 fan-out 작업이 이 스트림에 저장됨

    private static final String CONSUMER_GROUP = "fanout-group";
    // - Redis Stream의 컨슈머 그룹 이름
    // - 여러 컨슈머가 협력하여 메시지를 처리할 때 사용

    private static final String CONSUMER_NAME = "fanout-consumer";
    // - 실제 메시지를 처리하는 컨슈머의 이름
    // - 장애 추적이나 모니터링에 활용

    private static final long CACHE_TTL_HOURS = 12;
    // - 뉴스피드 캐시의 유효 기간을 12시간으로 설정

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // Redis 연산을 위한 RedisTemplate 주입
    // 객체 직렬화/역직렬화 지원

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // String 타입을 위한 RedisTemplate
    // Stream 작업에 주로 사용

    @Autowired
    private FollowerDAO followerDAO;

    @Autowired
    private PostDAO postDAO;

    // Consumer Group 초기화
    // 애플리케이션 시작 시 컨슈머 그룹을 생성
    // 이미 존재하는 경우 예외가 발생하므로 catch로 처리
    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(FANOUT_STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
        } catch (Exception e) {
            log.warn("Consumer group might already exist: {}", e.getMessage());
        }
    }

    // Fan-out 작업을 Redis Stream에 등록
    // 하나의 게시글을 여러 팔로워들의 피드에 배포하는 작업
    public void enqueueFanOutPost(Post post) {
        try {
            // Redis Stream에 저장할 메시지 데이터를 담을 Map 생성
            Map<String, String> messageMap = new HashMap<>();

            // 필수 데이터인 게시물 ID와 작성자 ID를 Map에 저장
            messageMap.put("postId", post.getId().toString());
            messageMap.put("userId", post.getUser().getId().toString());

            // 게시물의 타임스탬프 결정
            // 1. 생성 시간이 있으면 생성 시간 사용
            // 2. 없다면 수정 시간 사용
            // 3. 둘 다 없다면 현재 시간 사용
            LocalDateTime timestamp = post.getCreatedAt() != null ?
                    post.getCreatedAt() :
                    (post.getUpdatedAt() != null ? post.getUpdatedAt() : LocalDateTime.now());

            // LocalDateTime을 Unix Epoch 시간(초)으로 변환
            // Unix Epoch: 1970년 1월 1일 00:00:00 UTC부터 경과한 초 수
            long epochSeconds = timestamp
                    .atZone(ZoneId.systemDefault())  // 시스템 기본 시간대 적용
                    .toInstant()                     // Instant 객체로 변환
                    .getEpochSecond();               // 초 단위 epoch 시간 추출

            // 변환된 타임스탬프를 문자열로 Map에 저장
            messageMap.put("timestamp", String.valueOf(epochSeconds));

            // Redis Stream에 메시지 추가
            // FANOUT_STREAM_KEY: 스트림의 키 이름
            // StreamRecords.newRecord(): 새로운 레코드 생성
            // ofMap(): Map 형태의 데이터를 레코드에 저장
            RecordId recordId = stringRedisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .in(FANOUT_STREAM_KEY)
                            .ofMap(messageMap));

            // 작업 성공 로그 기록
            // 어떤 게시물이 어떤 사용자에 의해 언제 큐에 들어갔는지 기록
            log.info("Fan-out task enqueued - PostId: {}, UserId: {}, Timestamp: {}, RecordId: {}",
                    post.getId(), post.getUser().getId(), epochSeconds, recordId);

        } catch (Exception e) {
            // 예외 발생 시 에러 로그 기록 및 RuntimeException으로 래핑하여 재발생
            log.error("Failed to enqueue fan-out task for post: {}", post.getId(), e);
            throw new RuntimeException("Failed to enqueue fan-out task", e);
        }
    }

    // Fan-out 작업을 처리하는 Consumer
    /**
     * Redis Stream에서 Fan-out 작업을 주기적으로 처리하는 스케줄링 메소드
     * fixedDelay = 10: 이전 작업 완료 후 10ms 대기 후 다음 작업 시작
     */
    @Scheduled(fixedDelay = 10)
    public void processFanOutTasks() {
        try {
            // Redis Stream에서 읽기 위한 옵션 설정
            // count(10): 한 번에 최대 10개의 레코드만 읽음
            StreamReadOptions readOptions = StreamReadOptions.empty().count(10);

            // 스트림에서 레코드 읽기
            // Consumer.from(): 컨슈머 그룹과 컨슈머 이름 지정
            // StreamOffset.create(): 스트림 키와 읽기 시작할 오프셋 지정
            // ReadOffset.lastConsumed(): 마지막으로 처리한 위치부터 읽기
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            readOptions,
                            StreamOffset.create(FANOUT_STREAM_KEY, ReadOffset.lastConsumed()));

            // 처리할 레코드가 없으면 메소드 종료
            if (records == null || records.isEmpty()) {
                return;
            }

            log.info("Processing {} fan-out tasks", records.size());

            // 각 레코드 처리
            for (MapRecord<String, Object, Object> record : records) {
                try {
                    // 레코드에서 필요한 데이터 추출
                    Map<Object, Object> values = record.getValue();
                    Long postId = Long.parseLong((String) values.get("postId"));
                    Long userId = Long.parseLong((String) values.get("userId"));

                    log.info("Processing fan-out for PostId: {}, UserId: {}", postId, userId);

                    // 게시물 조회
                    Post post = postDAO.findById(postId)
                            .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

                    // 작성자의 팔로워 목록 조회
                    List<Follower> followers = followerDAO.findFollowers(userId);
                    log.info("Found {} followers for userId: {}", followers.size(), userId);

                    // 정렬을 위한 score 값으로 타임스탬프 사용
                    double score = Double.parseDouble((String) values.get("timestamp"));

                    // 1. 작성자의 뉴스 피드에 게시물 추가
                    String authorFeedKey = NEWS_FEED_KEY_PREFIX + userId;
                    // Redis Sorted Set에 추가 (키: 뉴스피드키, 값: 게시물ID, score: 타임스탬프)
                    redisTemplate.opsForZSet().add(authorFeedKey, postId.toString(), score);
                    // 뉴스피드 캐시의 만료 시간 설정
                    redisTemplate.expire(authorFeedKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

                    log.info("Added to author's feed: {}", authorFeedKey);

                    // 2. 각 팔로워의 뉴스 피드에 게시물 추가
                    for (Follower follower : followers) {
                        String newsFeedKey = NEWS_FEED_KEY_PREFIX + follower.getFollower().getId();
                        redisTemplate.opsForZSet().add(newsFeedKey, postId.toString(), score);
                        redisTemplate.expire(newsFeedKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

                        log.info("Added to follower's feed: {}", newsFeedKey);
                    }

                    // 성공적으로 처리된 레코드를 컨슈머 그룹에 승인
                    // 이를 통해 해당 레코드가 정상적으로 처리되었음을 알림
                    stringRedisTemplate.opsForStream()
                            .acknowledge(CONSUMER_GROUP, record);

                    log.info("Successfully processed and acknowledged record: {}", record.getId());

                } catch (Exception e) {
                    // 개별 레코드 처리 실패 시 해당 레코드만 스킵하고 계속 진행
                    log.error("Failed to process fan-out task: {}", record.getId(), e);
                }
            }
        } catch (Exception e) {
            // 전체 프로세스 실패 시 에러 로깅
            log.error("Error in fan-out task processing", e);
        }
    }

    // 모든 뉴스피드에서 특정 게시물을 제거하는 메서드
    @Transactional
    public void removePostFromNewsFeeds(Long postId) {
        try {
            String postIdStr = postId.toString();
            Set<String> keys = redisTemplate.keys(NEWS_FEED_KEY_PREFIX + "*");

            if (keys != null) {
                for (String key : keys) {
                    redisTemplate.opsForZSet().remove(key, postIdStr);
                }
            }
        } catch (Exception e) {
            log.error("Failed to remove post from news feeds: {}", postId, e);
            throw new RuntimeException("Failed to remove post from news feeds", e);
        }
    }

    // 사용자의 뉴스 피드를 가져오는 메서드
    public Page<PostDTO> getNewsFeed(Long userId, Pageable pageable) {
        String newsFeedKey = NEWS_FEED_KEY_PREFIX + userId;

        // Redis에서 캐시된 데이터 확인
        Set<Object> postIdsObj = redisTemplate.opsForZSet().reverseRange(
                newsFeedKey,
                pageable.getOffset(),
                pageable.getOffset() + pageable.getPageSize() - 1
        );

        // 캐시 미스 또는 캐시 검증 필요
        if (shouldRefreshCache(userId, newsFeedKey, postIdsObj)) {
            log.info("Cache refresh needed for user {}, generating from DB", userId);
            return generateNewsFeedFromDB(userId, pageable, newsFeedKey);
        }

        // 캐시된 데이터 반환
        List<Long> postIds = postIdsObj.stream()
                .map(obj -> Long.parseLong((String) obj))
                .toList();

        List<Post> posts = postDAO.findByIdIn(postIds);
        List<PostDTO> postDTOs = posts.stream()
                .map(this::convertToDTO)
                .toList();

        long totalElements = Optional.ofNullable(redisTemplate.opsForZSet().size(newsFeedKey))
                .orElse(0L);

        return new PageImpl<>(postDTOs, pageable, totalElements);
    }

    private boolean shouldRefreshCache(Long userId, String newsFeedKey, Set<Object> cachedPostIds) {
        // 캐시가 비어있거나 null인 경우 갱신 필요
        if (cachedPostIds == null || cachedPostIds.isEmpty()) {
            return true;
        }

        try {
            // 사용자가 팔로우하는 계정들의 ID 목록 조회
            // Stream을 사용하여 Following 엔티티에서 ID만 추출
            List<Long> followingIds = followerDAO.findFollowing(userId).stream()
                    .map(following -> following.getFollowing().getId())
                    .collect(Collectors.toList());

            // 자신의 게시물도 피드에 포함되어야 하므로 팔로잉 목록에 자신의 ID도 추가
            followingIds.add(userId);

            // DB에서 팔로잉하는 사용자들의 최신 게시물 1개만 조회
            // PageRequest.of(0, 1): 첫 페이지에서 1개만 가져옴
            Page<Post> latestPosts = postDAO.findByUserIdInOrderByUpdatedAtDesc(
                    followingIds,
                    PageRequest.of(0, 1)
            );

            // DB에 게시물이 없는 경우 캐시 갱신 불필요
            if (latestPosts.isEmpty()) {
                return false;
            }

            // 최신 게시물의 ID 추출
            Post latestPost = latestPosts.getContent().get(0);
            String latestPostId = latestPost.getId().toString();

            // Redis에서 캐시된 게시물 중 가장 최신 게시물 확인
            // reverseRange(0, 0): Score가 가장 높은(최신) 항목 1개 조회
            Set<Object> topCachedPost = redisTemplate.opsForZSet().reverseRange(newsFeedKey, 0, 0);

            // 캐시에 게시물이 없는 경우 갱신 필요
            if (topCachedPost == null || topCachedPost.isEmpty()) {
                return true;
            }

            // 캐시된 최신 게시물의 ID 추출
            String cachedLatestPostId = (String) topCachedPost.iterator().next();

            // DB의 최신 게시물 ID와 캐시의 최신 게시물 ID 비교
            // 다르다면 새로운 게시물이 있다는 의미이므로 캐시 갱신 필요
            return !latestPostId.equals(cachedLatestPostId);

        } catch (Exception e) {
            // 에러 발생 시 로깅하고, 안전을 위해 캐시 갱신 수행
            log.error("Error checking cache validity for user {}", userId, e);
            return true;
        }
    }


    // DB에서 뉴스피드 데이터를 생성하고 Redis에 저장하는 메서드
    private Page<PostDTO> generateNewsFeedFromDB(Long userId, Pageable pageable, String newsFeedKey) {
        // 새로운 데이터를 캐시하기 전에 기존 캐시 삭제
        redisTemplate.delete(newsFeedKey);

        // 사용자가 팔로우하는 계정들의 ID 목록 조회
        // Stream API를 사용하여 Following 엔티티에서 팔로잉하는 사용자의 ID만 추출
        List<Long> followingIds = followerDAO.findFollowing(userId).stream()
                .map(following -> following.getFollowing().getId())
                .collect(Collectors.toList());

        // 자신의 게시물도 피드에 포함되어야 하므로 팔로잉 목록에 자신의 ID 추가
        followingIds.add(userId);

        // DB에서 팔로잉하는 사용자들의 게시물을 최신순으로 조회
        // pageable 파라미터를 통해 페이지 크기와 offset이 지정됨
        Page<Post> postsPage = postDAO.findByUserIdInOrderByUpdatedAtDesc(followingIds, pageable);

        // 조회된 게시물들을 Redis Sorted Set에 캐시
        postsPage.getContent().forEach(post -> {
            // 정렬 기준이 될 타임스탬프 결정 (수정시간 우선, 없으면 생성시간 사용)
            LocalDateTime timeStamp = post.getUpdatedAt() != null ?
                    post.getUpdatedAt() :
                    post.getCreatedAt();

            // LocalDateTime을 Unix timestamp(초)로 변환하여 score로 사용
            double score = timeStamp
                    .atZone(ZoneId.systemDefault())  // 시스템 기본 시간대 적용
                    .toInstant()                     // Instant 객체로 변환
                    .getEpochSecond();               // 초 단위 epoch 시간 추출

            // Redis Sorted Set에 게시물 추가
            // 키: 뉴스피드 키, 값: 게시물ID, score: 타임스탬프
            redisTemplate.opsForZSet().add(newsFeedKey, post.getId().toString(), score);
            log.info("Added post {} to cache for user {} with score {}", post.getId(), userId, score);
        });

        // 캐시의 만료 시간 설정 (TTL)
        redisTemplate.expire(newsFeedKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

        // Post 엔티티를 PostDTO로 변환
        // 최신 Java 버전의 Stream.toList() 사용
        List<PostDTO> postDTOs = postsPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        // DTO 리스트를 페이징 정보와 함께 반환
        return new PageImpl<>(postDTOs, pageable, postsPage.getTotalElements());
    }
    // Post 엔티티 -> PostDTO 변환하는 메서드
    private PostDTO convertToDTO(Post post) {
        User user = post.getUser();
        UserDTO userDTO = new UserDTO(user.getId(), user.getUserName(), user.getEmail()); // 사용자 정보 변환
        return new PostDTO(post.getId(), post.getCaption(), post.getCreatedAt(), post.getUpdatedAt(), userDTO); // PostDTO 객체 생성 및 반환
    }
}
