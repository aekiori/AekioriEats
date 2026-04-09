package com.delivery.user.constant;

public class UserEventType {
    // usercreated는 auth 로 부터 'consume' 하는 이벤트임.
    // 내부적으로 처리/외부로부터 받는 이벤트에 대한 별도 구분을 가져갈까?
    public static final String USER_CREATED = "UserCreated";
    public static final String USER_STATUS_CHANGED = "UserStatusChanged";

    private UserEventType() {
    }
}
