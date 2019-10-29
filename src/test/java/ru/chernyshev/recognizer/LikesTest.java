package ru.chernyshev.recognizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.ChatRepository;
import ru.chernyshev.recognizer.repository.LikeRepository;
import ru.chernyshev.recognizer.repository.MessageRepository;
import ru.chernyshev.recognizer.repository.UserRepository;
import ru.chernyshev.recognizer.service.MessageRatingService;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {LikesTest.TestConfiguration.class, RepoFactory4Test.class})
@AutoConfigureMockMvc
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class})


public class LikesTest {

    private MessageEntity message;
    private UserEntity user;

    @Configuration
    static class TestConfiguration {
        @Bean
        MessageRatingService ratingService(LikeRepository likeRepository, MessageRepository messageRepository) {
//            return new MessageRatingService(likeRepository, messageRepository, userService);
            return null;
        }
    }

    @Autowired
    MessageRatingService ratingService;

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    UserRepository userRepository;
    @Autowired
    LikeRepository likeRepository;

    @Before
    public void init() {
        ChatEntity chat = FixtureHelper.createChat();
        chatRepository.save(chat);

        message = FixtureHelper.createMessage(chat);
        messageRepository.save(message);

        user = FixtureHelper.createUserEntity();
        userRepository.save(user);
    }

    @Test
    public void testLikeMessage() {
        ratingService.addLike(message, user, 4);
        Iterable<LikeEntity> allLikes = likeRepository.findAll();

        assertTrue(allLikes.iterator().hasNext());

        LikeEntity likeEntity = allLikes.iterator().next();
        assertThat(likeEntity.getMessage().getId(), is(1L));
        assertThat(likeEntity.getUser().getId(), is(1L));

//        Optional<MessageEntity> checkedMessage = messageRepository.findById(1L);
//        assertTrue(checkedMessage.isPresent());
//        Hibernate.initialize(checkedMessage.get().getLikes());
//        assertThat(checkedMessage.get().getLikes().size(), is(1));
    }
}
