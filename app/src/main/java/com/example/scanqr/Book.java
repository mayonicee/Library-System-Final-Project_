package com.example.scanqr;
public class Book {

    private String id;
    private String tittle;
    private String author;
    private String coverUrl;
    private String categoryId;
    private long availableCopies;
    private long totalCopies;
    private String qrCodeValue;
    private String sypnosis;

    public Book() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTittle() { return tittle; }
    public void setTittle(String tittle) { this.tittle = tittle; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public long getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(long availableCopies) { this.availableCopies = availableCopies; }

    public long getTotalCopies() { return totalCopies; }
    public void setTotalCopies(long totalCopies) { this.totalCopies = totalCopies; }

    public String getQrCodeValue() { return qrCodeValue; }
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }

    public String getSypnosis() { return sypnosis; }
    public void setSypnosis(String sypnosis) { this.sypnosis = sypnosis; }
}
