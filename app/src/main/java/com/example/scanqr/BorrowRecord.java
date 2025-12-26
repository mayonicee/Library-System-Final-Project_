package com.example.scanqr;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class BorrowRecord {

    private DocumentReference docRef;

    private String bookId;
    private String bookTittle;
    private String coverUrl;

    private String borrowerName;
    private String borrowerId;
    private String userUid;

    private String status;        // PENDING / BORROWED / REJECTED / RETURNED / LATE
    private String guarantee;
    private String locationType;  // INSIDE / OUTSIDE

    private Timestamp borrowedAt;
    private Timestamp dueDate;
    private Timestamp returnedAt;

    private long durationDays;

    public BorrowRecord() {}

    public DocumentReference getDocRef() {
        return docRef;
    }

    public void setDocRef(DocumentReference docRef) {
        this.docRef = docRef;
    }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBookTittle() { return bookTittle; }
    public void setBookTittle(String bookTittle) { this.bookTittle = bookTittle; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGuarantee() { return guarantee; }
    public void setGuarantee(String guarantee) { this.guarantee = guarantee; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public Timestamp getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(Timestamp borrowedAt) { this.borrowedAt = borrowedAt; }

    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }

    public Timestamp getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Timestamp returnedAt) { this.returnedAt = returnedAt; }

    public long getDurationDays() { return durationDays; }
    public void setDurationDays(long durationDays) { this.durationDays = durationDays; }
}
