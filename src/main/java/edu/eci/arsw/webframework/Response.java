package edu.eci.arsw.webframework;

public class Response {
    private String contentType = "text/html; charset=utf-8";
    private int statusCode = 200;                             // valor por defecto

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getContentType() {
        return this.contentType;
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }
    public int getStatus() {
        return this.statusCode;
    }
}
