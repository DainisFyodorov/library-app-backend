package com.dainis.spring_boot_library.controller;

import com.dainis.spring_boot_library.entity.Book;
import com.dainis.spring_boot_library.service.BookService;
import com.dainis.spring_boot_library.utils.ExtractJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("http://localhost:3000")
@RestController
@RequestMapping("/api/books")
public class BookController {

    private BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/secure/currentloans/count")
    public int currentLoansCount(@RequestHeader(value = "Authorization") String token) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, "\"email\"");
        return bookService.currentLoansCount(userEmail);
    }

    @GetMapping("/secure/ischeckedout/byuser")
    public Boolean checkoutBookByUser(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam Long bookId) {
        String userEmail = jwt.getClaim("email");
        return bookService.checkoutBookByUser(userEmail, bookId);
    }

    @PutMapping("/secure/checkout")
    public Book checkoutBook(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam Long bookId) throws Exception {
        String userEmail = jwt.getClaim("email");
        return bookService.checkoutBook(userEmail, bookId);
    }
}
