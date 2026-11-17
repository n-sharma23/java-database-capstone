package com.project.back_end.DTO;

/** Credentials posted by doctors and patients; the identifier is their email. */
public class Login {

    private String email;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
