package com.example.scanqr;

import com.google.firebase.Timestamp;

public class CollectionItem {

    private String id;         // doc id di /users/{uid}/collections/{id} (kita pakai bookId/docId)
    private String bookId;     // documentId di koleksi "books"
    private String bookTitle;
    private String coverUrl;
    private String status;     // FAVORITE, TO_READ, READING, FINISHED, dll
    private Timestamp lastOpenedAt;

    public CollectionItem() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getLastOpenedAt() { return lastOpenedAt; }
    public void setLastOpenedAt(Timestamp lastOpenedAt) { this.lastOpenedAt = lastOpenedAt; }
}
