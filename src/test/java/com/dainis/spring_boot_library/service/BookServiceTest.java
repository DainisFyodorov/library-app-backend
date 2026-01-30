package com.dainis.spring_boot_library.service;

import com.dainis.spring_boot_library.dao.BookRepository;
import com.dainis.spring_boot_library.dao.CheckoutRepository;
import com.dainis.spring_boot_library.dao.HistoryRepository;
import com.dainis.spring_boot_library.dao.PaymentRepository;
import com.dainis.spring_boot_library.entity.Book;
import com.dainis.spring_boot_library.entity.Checkout;
import com.dainis.spring_boot_library.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.LongBuffer;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CheckoutRepository checkoutRepository;

    @Mock
    private HistoryRepository historyRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("Successfully checkout the book without fees and book is available")
    void testCheckoutBookSuccess() throws Exception {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setId(bookId);
        book.setCopiesAvailable(10);
        book.setTitle("Test book");

        Payment payment = new Payment();
        payment.setUserEmail(userEmail);
        payment.setAmount(0.00);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);
        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(payment);

        Book result = bookService.checkoutBook(userEmail, bookId);

        assertNotNull(result);
        assertEquals(9, result.getCopiesAvailable(), "Amount of books available should be decreased by 1, equals 9");

        verify(bookRepository, times(1)).save(any(Book.class));
        verify(checkoutRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Checkout book which is not found")
    void testCheckoutBookNotFound() {
        Long bookId = 999L;
        String userEmail = "test@email.com";

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> bookService.checkoutBook(userEmail, bookId));

        assertTrue(exception.getMessage().contains("Book doesn't exist"));
        verify(checkoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout book if no copies are available")
    void testCheckoutBookNoCopiesAvailable() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setCopiesAvailable(0);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        Exception exception = assertThrows(Exception.class, () -> {
            bookService.checkoutBook(userEmail, bookId);
        });

        assertTrue(exception.getMessage().contains("Book doesn't exist or already checked out"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout book when user has outstanding fees")
    void testCheckoutBookHavingOutstandingFees() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setCopiesAvailable(5);

        Payment payment = new Payment();
        payment.setAmount(10.50);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(payment);
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);
        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(Collections.emptyList());

        assertThrows(Exception.class, () -> bookService.checkoutBook(userEmail, bookId));

        verify(bookRepository, never()).save(any());
        verify(checkoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout book which is already checked out by user")
    void testCheckoutBookWhichIsAlreadyCheckedOut() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setId(bookId);
        book.setCopiesAvailable(5);

        Checkout checkout = new Checkout();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);

        assertThrows(Exception.class, () -> bookService.checkoutBook(userEmail, bookId));

        verify(bookRepository, never()).save(any());
        verify(checkoutRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout book when having overdue books")
    void testCheckoutWithLateBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setId(bookId);
        book.setCopiesAvailable(5);

        Checkout overdueCheckout = new Checkout();
        overdueCheckout.setReturnDate(LocalDate.now().minusDays(2).toString());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(List.of(overdueCheckout));

        Payment payment = new Payment();
        payment.setAmount(0.00);

        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(payment);

        Exception exception = assertThrows(Exception.class, () -> bookService.checkoutBook(userEmail, bookId));

        assertEquals("Outstanding fees", exception.getMessage());

        verify(bookRepository, never()).save(any());
        verify(checkoutRepository, never()).save(any());
    }
}
