package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImage;
    private String status;
    private boolean isEmailVerified;
    private LocalDateTime createdAt;
}

