package edu.upenn.cis.cis455.m2.handling;

public class HttpCookie {
    
    private String name;
    private String value;
    private int maxAge;
    private boolean secured;
    private boolean httpOnly;
    private String path;

    public HttpCookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        // if (path == null) {
        //     path = "/";
        //     throw new IllegalArgumentException("Illegal Argument Exception: null path");
        // } else {
        //     path = "/" + path;
        // }
        this.path = path;
        this.name = name;
        this.value = value;
        this.maxAge = maxAge;
        this.secured = secured;
        this.httpOnly = httpOnly;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(name + "=" + value);
		sb.append(String.format(";path=\"%s\"", path));
        if (secured) {
            sb.append(";Secure");
        }
        if (httpOnly) {
            sb.append(";HttpOnly");
        }
        if (maxAge >= 0) {
            sb.append(";maxAge=" + maxAge);
        }
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean getSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean getHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
