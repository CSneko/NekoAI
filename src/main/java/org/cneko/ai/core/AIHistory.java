package org.cneko.ai.core;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AIHistory {
    private static final Gson GSON = new Gson();
    @SerializedName("contents")
    private List<Content> contents = new ArrayList<>();

    // getters and setters
    public List<Content> getContents() {return contents;}
    public void setContents(List<Content> contents) {this.contents = contents;}

    public static class Content{
        @SerializedName("role")
        private Role role = Role.USER;
        @SerializedName("parts")
        private List<Part> parts = new ArrayList<>();

        // getters and setters
        public Role getRole() {return role;}
        public void setRole(Role role) {this.role = role;}
        public List<Part> getParts() {return parts;}
        public void setParts(List<Part> parts) {this.parts = parts;}

        public static class Part{
            @SerializedName("text")
            private String text = "";

            public Part(String text) {
                this.text = text;
            }

            // getters and setters
            public String getText() {return text;}
            public void setText(String text) {this.text = text;}
        }

        public enum Role{
            @SerializedName("user")
            USER,
            @SerializedName("model")
            MODEL
        }
        public static Content create(Role role, String text){
            Content content = new Content();
            content.setRole(role);
            content.setParts(List.of(new Content.Part(text)));
            return content;
        }
    }

    public static AIHistory fromJson(String json){
        return GSON.fromJson(json, AIHistory.class);
    }
    public String toJson(){
        return GSON.toJson(this);
    }
}
