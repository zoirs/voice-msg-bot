package ru.chernyshev.recognizer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.UserRepository;

@Service
public class UserService {
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
        }
        return userEntity;
    }
}
