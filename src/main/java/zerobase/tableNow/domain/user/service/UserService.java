package zerobase.tableNow.domain.user.service;

import zerobase.tableNow.domain.user.dto.DeleteDto;
import zerobase.tableNow.domain.user.dto.LoginDto;
import zerobase.tableNow.domain.user.dto.RePasswordDto;
import zerobase.tableNow.domain.user.dto.RegisterDto;

public interface UserService {
    //회원가입
    RegisterDto register(RegisterDto registerDto);

    //이메일 인증
    boolean emailAuth(String userId, String authKey);

    //로그인
    LoginDto login(LoginDto loginDto);


    //비밀번호 재설정
    String rePassword(RePasswordDto rePasswordDto);

    //회원 탈퇴
    DeleteDto userDelete(String userId);
}
