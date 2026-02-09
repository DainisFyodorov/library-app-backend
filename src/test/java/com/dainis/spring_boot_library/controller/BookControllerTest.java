package com.dainis.spring_boot_library.controller;

import com.dainis.spring_boot_library.dao.BookRepository;
import com.dainis.spring_boot_library.entity.Book;
import com.dainis.spring_boot_library.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource("/application.properties")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
public class BookControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    public void setupData() {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setDescription("Test Description");
        book.setAuthor("Test Author");
        book.setCopies(10);
        book.setCopiesAvailable(10);
        book.setCategory("BE");
        book.setImg("Test Image");

        bookRepository.save(book);
    }

    @Test
    public void checkoutBookHttpRequest() throws Exception {
        Long bookId = bookRepository.findAll().getFirst().getId();

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", bookId.toString())
                        .with(jwt().jwt(j -> j.claim("email", "test@example.com"))))
                .andExpect(status().isOk());

        assertEquals(9, bookRepository.findById(bookId).get().getCopiesAvailable());
    }

    @AfterEach
    public void setupAfterTransaction() {
        jdbcTemplate.execute("delete from book");
        jdbcTemplate.execute("alter table book alter column id restart with 1");
    }
}
