package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.UserRepository;

import java.util.Objects;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getOrCreate(User user) {
        UserEntity userEntity = userRepository.findByTelegramId(user.getId());
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setTelegramId(user.getId());
            userEntity.setFirstName(user.getFirstName());
            userEntity.setUserName(user.getUserName());
            userEntity.setLastName(user.getLastName());
            userRepository.save(userEntity);
        } else if (isNameWasChanged(user, userEntity)) {
            userEntity.setFirstName(user.getFirstName());
            userEntity.setUserName(user.getUserName());
            userEntity.setLastName(user.getLastName());
            userRepository.save(userEntity);
            String oldName = userEntity.getUserName() + " " + userEntity.getFirstName() + " " + userEntity.getLastName();
            String currentName = user.getUserName() + " " + user.getFirstName() + " " + user.getLastName();
            logger.info("Name of user was changed from {} to {}", oldName, currentName);
        }
        return userEntity;
    }

    private static boolean isNameWasChanged(User user, UserEntity userEntity) {
        return !Objects.equals(userEntity.getUserName(), user.getUserName())
                || !Objects.equals(userEntity.getFirstName(), user.getFirstName())
                || !Objects.equals(userEntity.getLastName(), user.getLastName());
    }
}
