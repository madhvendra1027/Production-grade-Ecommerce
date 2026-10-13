package com.example.project.payLoad;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class APIresponse {
    public String message;
    private boolean status;
}
