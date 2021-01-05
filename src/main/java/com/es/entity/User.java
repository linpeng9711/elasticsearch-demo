package com.es.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class User {

    private String first_name;

    private String last_name;

    private int age;

    private String about;

    private String[] interests;
}
