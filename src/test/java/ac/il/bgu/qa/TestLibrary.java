package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestLibrary {
    private static final String ISBN = "9780306406157";
    private static final String USER_ID = "123456789012";
    private static final String MESSAGE = "Reviews for 'Library Book':\nExcellent\nWorth reading";
    private DatabaseService database;
    private ReviewService reviews;
    private NotificationService notifications;
    private Library library;
    private Book book;
    private User user;

    @BeforeEach
    void setUp() {
        database = mock(DatabaseService.class);
        reviews = mock(ReviewService.class);
        notifications = mock(NotificationService.class);
        library = new Library(database, reviews);
        book = mock(Book.class);
        user = mock(User.class);
    }

    private void arrangeValidBook(String isbn, String title, String author) {
        when(book.getISBN()).thenReturn(isbn);
        when(book.getTitle()).thenReturn(title);
        when(book.getAuthor()).thenReturn(author);
    }

    private void arrangeValidUser() {
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn("Dana");
        when(user.getNotificationService()).thenReturn(notifications);
    }

    private void arrangeBookAndUser() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        when(database.getUserById(USER_ID)).thenReturn(user);
    }

    private void arrangeReviews() {
        arrangeBookAndUser();
        when(book.getTitle()).thenReturn("Library Book");
        when(reviews.getReviewsForBook(ISBN)).thenReturn(List.of("Excellent", "Worth reading"));
    }

    static Stream<String> invalidISBNs() {
        return Stream.of(null, "", "978030640615", "97803064061570", "9780306406158",
                "978030640615a", "a780306406157", "9780306406141",
                " 9780306406157", "9780306406157 ", "-------------");
    }

    static Stream<String> invalidUserIds() {
        return Stream.of(null, "", "12345678901", "1234567890123", "12345678901a",
                " 123456789012", "123456789012 ");
    }

    @Test
    void GivenNullBook_WhenAddBook_ThenRejectBook() {
        assertEquals("Invalid book.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @MethodSource("invalidISBNs")
    void GivenInvalidISBN_WhenAddBook_ThenRejectBook(String isbn) {
        arrangeValidBook(isbn, "Library Book", "Jane Austen");
        assertEquals("Invalid ISBN.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9780306406157", "978-0-306-40615-7", "9780306406102", "9780306406119", "9780306406140"})
    void GivenValidISBN_WhenAddBook_ThenStoreBook(String isbn) {
        arrangeValidBook(isbn, "Library Book", "Jane Austen");
        library.addBook(book);
        verify(database).getBookByISBN(isbn);
        verify(database).addBook(isbn, book);
        verifyNoMoreInteractions(database);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidTitle_WhenAddBook_ThenRejectBook(String title) {
        arrangeValidBook(ISBN, title, "Jane Austen");
        assertEquals("Invalid title.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1Jane", "Jane1", " Jane", "Jane ", "Jane1Austen", "Jane@Austen",
            "Jane--Austen", "Jane''Austen", "Jan@e", "-Jane", "Jane-", ".Jane", "Jane.", "'Jane", "Jane'"})
    void GivenInvalidAuthor_WhenAddBook_ThenRejectBook(String author) {
        arrangeValidBook(ISBN, "Library Book", author);
        assertEquals("Invalid author.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "Jane Austen", "Jean-Paul Sartre", "O'Connor", "J. R. R. Tolkien"})
    void GivenValidAuthor_WhenAddBook_ThenStoreBook(String author) {
        arrangeValidBook(ISBN, "Library Book", author);
        library.addBook(book);
        verify(database).addBook(ISBN, book);
    }

    @Test
    void GivenBorrowedBook_WhenAddBook_ThenRejectBook() {
        arrangeValidBook(ISBN, "Library Book", "Jane Austen");
        when(book.isBorrowed()).thenReturn(true);
        assertEquals("Book with invalid borrowed state.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book)).getMessage());
        verifyNoInteractions(database);
    }

    @Test
    void GivenExistingBook_WhenAddBook_ThenRejectDuplicate() {
        arrangeValidBook(ISBN, "Library Book", "Jane Austen");
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertEquals("Book already exists.", assertThrows(IllegalArgumentException.class,
                () -> library.addBook(book)).getMessage());
        verify(database, never()).addBook(anyString(), any());
    }

    @Test
    void GivenNullUser_WhenRegisterUser_ThenRejectUser() {
        assertEquals("Invalid user.", assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(null)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidId_WhenRegisterUser_ThenRejectUser(String id) {
        arrangeValidUser();
        when(user.getId()).thenReturn(id);
        assertEquals("Invalid user Id.", assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user)).getMessage());
        verifyNoInteractions(database);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidName_WhenRegisterUser_ThenRejectUser(String name) {
        arrangeValidUser();
        when(user.getName()).thenReturn(name);
        assertEquals("Invalid user name.", assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user)).getMessage());
        verifyNoInteractions(database);
    }

    @Test
    void GivenMissingNotificationService_WhenRegisterUser_ThenRejectUser() {
        arrangeValidUser();
        when(user.getNotificationService()).thenReturn(null);
        assertEquals("Invalid notification service.", assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user)).getMessage());
        verifyNoInteractions(database);
    }

    @Test
    void GivenExistingUser_WhenRegisterUser_ThenRejectDuplicate() {
        arrangeValidUser();
        when(database.getUserById(USER_ID)).thenReturn(user);
        assertEquals("User already exists.", assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(user)).getMessage());
        verify(database, never()).registerUser(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789012", "000000000000"})
    void GivenValidUser_WhenRegisterUser_ThenStoreUser(String id) {
        arrangeValidUser();
        when(user.getId()).thenReturn(id);
        library.registerUser(user);
        verify(database).getUserById(id);
        verify(database).registerUser(id, user);
        verifyNoMoreInteractions(database);
    }

    @ParameterizedTest
    @MethodSource("invalidISBNs")
    void GivenInvalidISBN_WhenBorrowBook_ThenRejectRequest(String isbn) {
        assertThrows(IllegalArgumentException.class, () -> library.borrowBook(isbn, USER_ID));
        verifyNoInteractions(database, book);
    }

    @Test
    void GivenMissingBook_WhenBorrowBook_ThenThrowBookNotFoundException() {
        assertThrows(BookNotFoundException.class, () -> library.borrowBook(ISBN, USER_ID));
        verify(database, never()).borrowBook(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidId_WhenBorrowBook_ThenRejectRequest(String id) {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertThrows(IllegalArgumentException.class, () -> library.borrowBook(ISBN, id));
        verifyNoInteractions(book);
        verify(database, never()).borrowBook(anyString(), any());
    }

    @Test
    void GivenUnregisteredUser_WhenBorrowBook_ThenThrowUserNotRegisteredException() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertThrows(UserNotRegisteredException.class, () -> library.borrowBook(ISBN, USER_ID));
        verify(book, never()).borrow();
        verify(database, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void GivenBorrowedBook_WhenBorrowBook_ThenThrowBookAlreadyBorrowedException() {
        arrangeBookAndUser();
        when(book.isBorrowed()).thenReturn(true);
        assertThrows(BookAlreadyBorrowedException.class, () -> library.borrowBook(ISBN, USER_ID));
        verify(book, never()).borrow();
        verify(database, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void GivenAvailableBookAndRegisteredUser_WhenBorrowBook_ThenBorrowAndRecord() {
        arrangeBookAndUser();
        library.borrowBook(ISBN, USER_ID);
        verify(book).borrow();
        verify(database).borrowBook(ISBN, USER_ID);
    }

    @ParameterizedTest
    @MethodSource("invalidISBNs")
    void GivenInvalidISBN_WhenReturnBook_ThenRejectRequest(String isbn) {
        assertThrows(IllegalArgumentException.class, () -> library.returnBook(isbn));
        verifyNoInteractions(database, book);
    }

    @Test
    void GivenMissingBook_WhenReturnBook_ThenThrowBookNotFoundException() {
        assertThrows(BookNotFoundException.class, () -> library.returnBook(ISBN));
        verify(database, never()).returnBook(anyString());
    }

    @Test
    void GivenAvailableBook_WhenReturnBook_ThenThrowBookNotBorrowedException() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertThrows(BookNotBorrowedException.class, () -> library.returnBook(ISBN));
        verify(book, never()).returnBook();
        verify(database, never()).returnBook(anyString());
    }

    @Test
    void GivenBorrowedBook_WhenReturnBook_ThenReturnAndRecord() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        when(book.isBorrowed()).thenReturn(true);
        library.returnBook(ISBN);
        verify(book).returnBook();
        verify(database).returnBook(ISBN);
    }

    @ParameterizedTest
    @MethodSource("invalidISBNs")
    void GivenInvalidISBN_WhenNotifyUserWithBookReviews_ThenRejectRequest(String isbn) {
        assertThrows(IllegalArgumentException.class, () -> library.notifyUserWithBookReviews(isbn, USER_ID));
        verifyNoInteractions(database, reviews, user);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidId_WhenNotifyUserWithBookReviews_ThenRejectRequest(String id) {
        assertThrows(IllegalArgumentException.class, () -> library.notifyUserWithBookReviews(ISBN, id));
        verifyNoInteractions(database, reviews, user);
    }

    @Test
    void GivenMissingBook_WhenNotifyUserWithBookReviews_ThenThrowBookNotFoundException() {
        assertThrows(BookNotFoundException.class, () -> library.notifyUserWithBookReviews(ISBN, USER_ID));
        verifyNoInteractions(reviews, user);
    }

    @Test
    void GivenUnregisteredUser_WhenNotifyUserWithBookReviews_ThenThrowUserNotRegisteredException() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertThrows(UserNotRegisteredException.class, () -> library.notifyUserWithBookReviews(ISBN, USER_ID));
        verifyNoInteractions(reviews, user);
    }

    static Stream<List<String>> missingReviews() {
        return Stream.of(null, Collections.emptyList());
    }

    @ParameterizedTest
    @MethodSource("missingReviews")
    void GivenNoReviews_WhenNotifyUserWithBookReviews_ThenThrowAndCloseService(List<String> values) {
        arrangeBookAndUser();
        when(reviews.getReviewsForBook(ISBN)).thenReturn(values);
        assertThrows(NoReviewsFoundException.class, () -> library.notifyUserWithBookReviews(ISBN, USER_ID));
        verify(reviews).close();
        verify(user, never()).sendNotification(anyString());
    }

    @Test
    void GivenReviewServiceFailure_WhenNotifyUserWithBookReviews_ThenThrowAndCloseService() {
        arrangeBookAndUser();
        when(reviews.getReviewsForBook(ISBN)).thenThrow(new ReviewException("Unavailable"));
        assertThrows(ReviewServiceUnavailableException.class, () -> library.notifyUserWithBookReviews(ISBN, USER_ID));
        verify(reviews).close();
        verify(user, never()).sendNotification(anyString());
    }

    @Test
    void GivenReviews_WhenNotifyUserWithBookReviews_ThenSendFormattedMessageAndCloseService() {
        arrangeReviews();
        library.notifyUserWithBookReviews(ISBN, USER_ID);
        verify(reviews).getReviewsForBook(ISBN);
        verify(reviews).close();
        verify(user).sendNotification(MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void GivenTransientNotificationFailures_WhenNotifyUserWithBookReviews_ThenRetryUntilSuccess(int failures) {
        arrangeReviews();
        int[] attempts = {0};
        doAnswer(invocation -> {
            if (attempts[0]++ < failures) {
                throw new NotificationException("Temporary failure");
            }
            return null;
        }).when(user).sendNotification(MESSAGE);
        library.notifyUserWithBookReviews(ISBN, USER_ID);
        verify(user, times(failures + 1)).sendNotification(MESSAGE);
        verify(reviews).getReviewsForBook(ISBN);
        verify(reviews).close();
    }

    @Test
    void GivenPersistentNotificationFailure_WhenNotifyUserWithBookReviews_ThenStopAfterFiveAttempts() {
        arrangeReviews();
        doThrow(new NotificationException("Unavailable")).when(user).sendNotification(MESSAGE);
        assertEquals("Notification failed!", assertThrows(NotificationException.class,
                () -> library.notifyUserWithBookReviews(ISBN, USER_ID)).getMessage());
        verify(user, times(5)).sendNotification(MESSAGE);
        verify(reviews).close();
    }

    @ParameterizedTest
    @MethodSource("invalidISBNs")
    void GivenInvalidISBN_WhenGetBookByISBN_ThenRejectRequest(String isbn) {
        assertThrows(IllegalArgumentException.class, () -> library.getBookByISBN(isbn, USER_ID));
        verifyNoInteractions(database, reviews);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidId_WhenGetBookByISBN_ThenRejectRequest(String id) {
        assertThrows(IllegalArgumentException.class, () -> library.getBookByISBN(ISBN, id));
        verifyNoInteractions(database, reviews);
    }

    @Test
    void GivenMissingBook_WhenGetBookByISBN_ThenThrowBookNotFoundException() {
        assertThrows(BookNotFoundException.class, () -> library.getBookByISBN(ISBN, USER_ID));
        verifyNoInteractions(reviews, user);
    }

    @Test
    void GivenBorrowedBook_WhenGetBookByISBN_ThenThrowBookAlreadyBorrowedException() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        when(book.isBorrowed()).thenReturn(true);
        assertThrows(BookAlreadyBorrowedException.class, () -> library.getBookByISBN(ISBN, USER_ID));
        verifyNoInteractions(reviews, user);
    }

    @Test
    void GivenAvailableBook_WhenGetBookByISBN_ThenReturnBookAndNotifyUser() {
        arrangeReviews();
        assertSame(book, library.getBookByISBN(ISBN, USER_ID));
        verify(user).sendNotification(MESSAGE);
        verify(reviews).close();
    }

    @Test
    void GivenNotificationFailure_WhenGetBookByISBN_ThenStillReturnBook() {
        arrangeReviews();
        doThrow(new NotificationException("Unavailable")).when(user).sendNotification(MESSAGE);
        assertSame(book, library.getBookByISBN(ISBN, USER_ID));
        verify(user, times(5)).sendNotification(MESSAGE);
    }

    @Test
    void GivenReviewFailure_WhenGetBookByISBN_ThenStillReturnBook() {
        arrangeBookAndUser();
        when(reviews.getReviewsForBook(ISBN)).thenThrow(new ReviewException("Unavailable"));
        assertSame(book, library.getBookByISBN(ISBN, USER_ID));
        verify(reviews).close();
        verify(user, never()).sendNotification(anyString());
    }

    @Test
    void GivenUnregisteredUser_WhenGetBookByISBN_ThenStillReturnBook() {
        when(database.getBookByISBN(ISBN)).thenReturn(book);
        assertSame(book, library.getBookByISBN(ISBN, USER_ID));
        verifyNoInteractions(reviews);
    }

    @Test
    void GivenSingleReview_WhenNotifyUserWithBookReviews_ThenSendMessageWithoutTrailingNewline() {
        arrangeBookAndUser();
        when(book.getTitle()).thenReturn("Another Book");
        when(reviews.getReviewsForBook(ISBN)).thenReturn(List.of("Only review"));

        library.notifyUserWithBookReviews(ISBN, USER_ID);

        verify(user).sendNotification("Reviews for 'Another Book':\nOnly review");
        verify(reviews).close();
    }

    @Test
    void GivenUnexpectedReviewFailure_WhenNotifyUserWithBookReviews_ThenPropagateAndCloseService() {
        arrangeBookAndUser();
        IllegalStateException failure = new IllegalStateException("Unexpected failure");
        when(reviews.getReviewsForBook(ISBN)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> library.notifyUserWithBookReviews(ISBN, USER_ID)));

        verify(reviews).close();
        verifyNoInteractions(user);
    }

    @ParameterizedTest
    @MethodSource("missingReviews")
    void GivenNoReviews_WhenGetBookByISBN_ThenStillReturnBook(List<String> values) {
        arrangeBookAndUser();
        when(reviews.getReviewsForBook(ISBN)).thenReturn(values);

        assertSame(book, library.getBookByISBN(ISBN, USER_ID));

        verify(reviews).close();
        verifyNoInteractions(user);
    }
}
