package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.enumClass.UserType;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.service.convert.UserConvert;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务：登录、注册、查询
 * 登录失败次数限制使用进程内内存实现（单机有效）。若部署多实例，应改用 Redis 共享计数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 允许的最大连续失败次数 */
    private static final int MAX_ATTEMPTS = 5;
    /** 锁定/统计窗口：15 分钟 */
    private static final long ATTEMPT_WINDOW_MILLIS = 15 * 60 * 1000L;

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录失败记录：username -> 失败次数与最近一次失败时间 */
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        String username = commandDTO.getUsername();
        checkLoginLock(username);

        // 用户名或邮箱均可登录
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .or()
                .eq(User::getEmail, username);
        User user = userMapper.selectOne(queryWrapper);

        // 统一返回「用户名或密码错误」，避免暴露用户是否存在
        if (user == null || !passwordEncoder.matches(commandDTO.getPassword().trim(), user.getPassword())) {
            recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        clearFailures(username);
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToLoginResponse(token, userInfo);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, commandDTO.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }

        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, commandDTO.getEmail())) > 0) {
            throw new BusinessException("邮箱已存在");
        }

        if (!UserType.isValidCode(commandDTO.getUserType())) {
            throw new BusinessException("无效的用户类型");
        }

        String encodedPassword = passwordEncoder.encode(commandDTO.getPassword().trim());
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);
        userMapper.insert(user);
        log.info("新用户注册成功: {}", user.getUsername());

        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }

    // ------------------------------------------------------------
    // 登录失败次数限制
    // ------------------------------------------------------------

    private void checkLoginLock(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return;
        }
        // 超过窗口期则重置
        if (System.currentTimeMillis() - attempt.lastAttempt > ATTEMPT_WINDOW_MILLIS) {
            loginAttempts.remove(username);
            return;
        }
        if (attempt.count >= MAX_ATTEMPTS) {
            long remainingMinutes = (ATTEMPT_WINDOW_MILLIS - (System.currentTimeMillis() - attempt.lastAttempt)) / 60000;
            throw new BusinessException("登录失败次数过多，请 " + Math.max(remainingMinutes, 1) + " 分钟后再试");
        }
    }

    private void recordFailure(String username) {
        loginAttempts.compute(username, (key, old) -> {
            LoginAttempt attempt = old == null ? new LoginAttempt() : old;
            attempt.count++;
            attempt.lastAttempt = System.currentTimeMillis();
            return attempt;
        });
    }

    private void clearFailures(String username) {
        loginAttempts.remove(username);
    }

    private static class LoginAttempt {
        private int count;
        private long lastAttempt;
    }
}
