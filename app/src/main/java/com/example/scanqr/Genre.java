package com.example.scanqr;

public class Genre {

    // isi dengan categoryId, misal: "fiction"
    private String name;

    public Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
