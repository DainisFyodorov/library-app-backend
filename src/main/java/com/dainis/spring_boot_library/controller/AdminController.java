package com.dainis.spring_boot_library.controller;

import com.dainis.spring_boot_library.requestmodels.AddBookRequest;
import com.dainis.spring_boot_library.service.AdminService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("http://localhost:3000")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/secure/add/book")
    public void postBook(@AuthenticationPrincipal Jwt jwt,
                         @RequestBody AddBookRequest addBookRequest) throws Exception {
        String userType = jwt.getClaim("userType");

        if(userType == null || !userType.equals("admin")) {
            throw new Exception("Administration page only");
        }

        adminService.postBook(addBookRequest);
    }
}
