package com.dainis.spring_boot_library.service;

import com.dainis.spring_boot_library.dao.BookRepository;
import com.dainis.spring_boot_library.dao.CheckoutRepository;
import com.dainis.spring_boot_library.dao.HistoryRepository;
import com.dainis.spring_boot_library.dao.PaymentRepository;
import com.dainis.spring_boot_library.entity.Book;
import com.dainis.spring_boot_library.entity.Checkout;
import com.dainis.spring_boot_library.entity.History;
import com.dainis.spring_boot_library.entity.Payment;
import com.dainis.spring_boot_library.responsemodels.ShelfCurrentLoansResponse;
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

    //region checkoutBook tests
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

    @Test
    @DisplayName("Create new payment record if not found during checkout")
    void testCheckoutBookPaymentNotFound() throws Exception {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setId(bookId);
        book.setCopiesAvailable(5);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(null);
        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(Collections.emptyList());

        bookService.checkoutBook(userEmail, bookId);

        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getUserEmail().equals(userEmail) && payment.getAmount() == 0.00
        ));

        verify(checkoutRepository, times(1)).save(any(Checkout.class));
    }
    //endregion

    //region checkoutBookByUser tests
    @DisplayName("Should return true if book is checked out")
    @Test
    void shouldReturnTrueIfBookIsCheckedOut() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(new Checkout());
        assertTrue(bookService.checkoutBookByUser(userEmail, bookId));
        verify(checkoutRepository).findByUserEmailAndBookId(userEmail, bookId);
    }

    @DisplayName("Should return false when book is not checked out")
    @Test
    void shouldReturnFalseWhenBookIsNotCheckedOut() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);
        assertFalse(bookService.checkoutBookByUser(userEmail, bookId));
        verify(checkoutRepository).findByUserEmailAndBookId(userEmail, bookId);
    }
    //endregion

    //region currentLoans tests

    @DisplayName("Should correctly calculate days left for loaned books")
    @Test
    void shouldReturnLoansWithCorrectCalculatedDays() throws Exception {
        String userEmail = "test@example.com";
        String tomorrow = LocalDate.now().plusDays(1).toString();

        Checkout checkout = new Checkout();
        checkout.setBookId(1L);
        checkout.setReturnDate(tomorrow);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Unit Test");

        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(List.of(checkout));
        when(bookRepository.findBooksByBookIds(List.of(1L))).thenReturn(List.of(book));

        List<ShelfCurrentLoansResponse> result = bookService.currentLoans(userEmail);

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getDaysLeft());
        assertEquals("Unit Test", result.getFirst().getBook().getTitle());
    }

    @DisplayName("Should return negative days when loan is overdue")
    @Test
    void shouldReturnNegativeDaysWhenLoanIsOverdue() throws Exception {
        String userEmail = "test@example.com";
        String yesterday = LocalDate.now().minusDays(1).toString();

        Checkout checkout = new Checkout();
        checkout.setBookId(1L);
        checkout.setReturnDate(yesterday);

        Book book = new Book();
        book.setId(1L);

        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(List.of(checkout));
        when(bookRepository.findBooksByBookIds(List.of(1L))).thenReturn(List.of(book));

        List<ShelfCurrentLoansResponse> result = bookService.currentLoans(userEmail);

        assertTrue(result.getFirst().getDaysLeft() < 0, "Days left should be negative for overdue books");
    }

    @DisplayName("Should return an empty array when checkout is empty")
    @Test
    void shouldReturnEmptyArrayWhenCheckoutIsEmpty() throws Exception {
        String userEmail = "test@example.com";

        when(checkoutRepository.findBooksByUserEmail(userEmail)).thenReturn(Collections.emptyList());
        when(bookRepository.findBooksByBookIds(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ShelfCurrentLoansResponse> result = bookService.currentLoans(userEmail);

        assertNotNull(result, "Should not be null");
        assertTrue(result.isEmpty(), "List should be empty");
    }

    //endregion

    //region returnBook tests (TODO)

    @DisplayName("Successfully return the book")
    @Test
    void testReturnBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setTitle("Test Book");
        book.setCopiesAvailable(1);

        Checkout checkout = new Checkout();
        checkout.setId(2L);
        checkout.setReturnDate(LocalDate.now().plusDays(1).toString());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);

        assertDoesNotThrow(() -> bookService.returnBook(userEmail, bookId));
        assertEquals(2, book.getCopiesAvailable());

        verify(bookRepository, times(1)).save(any());
        verifyNoInteractions(paymentRepository);
        verify(checkoutRepository, times(1)).deleteById(any());
        verify(historyRepository, times(1)).save(any());
    }

    @DisplayName("Return late book")
    @Test
    void testReturnLateBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setTitle("Test Book");
        book.setCopiesAvailable(1);

        Checkout checkout = new Checkout();
        checkout.setId(2L);
        checkout.setReturnDate(LocalDate.now().minusDays(1).toString());

        Payment payment = new Payment();
        payment.setAmount(100.00);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);
        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(payment);

        assertDoesNotThrow(() -> bookService.returnBook(userEmail, bookId));
        assertEquals(2, book.getCopiesAvailable());
        assertEquals(101.00, payment.getAmount(), 0.001);

        verify(paymentRepository, times(1)).save(payment);
        verify(bookRepository, times(1)).save(book);
        verify(checkoutRepository, times(1)).deleteById(checkout.getId());
        verify(historyRepository, times(1)).save(any());
    }

    @DisplayName("Return late book with no payment exists")
    @Test
    void testReturnLateBookWithoutExistingPayment() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setTitle("Test Book");
        book.setCopiesAvailable(1);

        Checkout checkout = new Checkout();
        checkout.setId(2L);
        checkout.setReturnDate(LocalDate.now().minusDays(1).toString());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);
        when(paymentRepository.findByUserEmail(userEmail)).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> bookService.returnBook(userEmail, bookId));

        assertTrue(exception.getMessage().contains("Payment doesn't exist"));
        assertEquals(1, book.getCopiesAvailable());

        verify(paymentRepository, never()).save(any());
        verify(bookRepository, never()).save(book);
        verify(checkoutRepository, never()).deleteById(checkout.getId());
        verifyNoInteractions(historyRepository);
    }

    @DisplayName("Return book which does not exist")
    @Test
    void testReturnNonExistingBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> bookService.returnBook(userEmail, bookId));

        assertTrue(exception.getMessage().contains("Book doesn't exist or not checked out by user"));
    }

    @DisplayName("Return book which is not checked out")
    @Test
    void testReturnNotCheckedOutBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Book book = new Book();
        book.setCopiesAvailable(1);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> bookService.returnBook(userEmail, bookId));

        assertEquals(1, book.getCopiesAvailable());
        assertTrue(exception.getMessage().contains("Book doesn't exist or not checked out by user"));

        verify(bookRepository, never()).save(book);
    }

    //endregion

    //region renewLoan tests

    @DisplayName("Successfully renew the loan")
    @Test
    void testRenewLoan() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Checkout checkout = new Checkout();
        checkout.setReturnDate(LocalDate.now().plusDays(1).toString());

        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);

        assertDoesNotThrow(() -> bookService.renewLoan(userEmail, bookId));
        assertEquals(LocalDate.now().plusDays(7).toString(), checkout.getReturnDate());

        verify(checkoutRepository, times(1)).save(any());
    }

    @DisplayName("Renew the loan of book which is not checkout")
    @Test
    void testRenewLoanOfNotCheckedOutBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> bookService.renewLoan(userEmail, bookId), "Should throw an exception");
        assertTrue(exception.getMessage().contains("Book does not exist or not checked out by user"));

        verify(checkoutRepository, never()).save(any());
    }

    @DisplayName("Renew the loan of book which is already delayed")
    @Test
    void testRenewLoanOfDelayedBook() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        Checkout checkout = new Checkout();
        checkout.setReturnDate(LocalDate.now().minusDays(1).toString());

        when(checkoutRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(checkout);

        assertDoesNotThrow(() -> bookService.renewLoan(userEmail, bookId));
        assertEquals(LocalDate.now().minusDays(1).toString(), checkout.getReturnDate());

        verify(checkoutRepository, never()).save(any());
    }

    //endregion
}
