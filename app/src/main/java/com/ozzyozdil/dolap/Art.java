package com.ozzyozdil.dolap;

public class Art {

    private String name;
    private int id;

    // Constructor
    public Art (String name, int id){
        this.setName(name);
        this.setId(id);
    }

    // Getter and Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
