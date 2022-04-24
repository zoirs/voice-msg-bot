package ru.chernyshev.recognizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.repository.ChatRepository;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ReposTest.EmployeeServiceImplTestContextConfiguration.class, RepoFactory4Test.class})
@AutoConfigureMockMvc
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class})
//https://stackoverflow.com/questions/27856266/how-to-make-instance-of-crudrepository-interface-during-testing-in-spring
public class ReposTest {

    @Configuration
    static class EmployeeServiceImplTestContextConfiguration {
    }

    @Autowired
    ChatRepository chatRepository;

    @Test
    public void givenGenericEntityRepository_whenSaveAndRetreiveEntity_thenOK() {
        ChatEntity s = new ChatEntity();
        s.setTelegramId(1L);
        s.setState(ChatStatus.ACTIVE);
        chatRepository.save(s);
        ChatEntity foundEntity = chatRepository.findByTelegramId(s.getTelegramId());

        assertNotNull(foundEntity);
    }
}
