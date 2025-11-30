package com.example.mini_projet.models;

import java.util.List;
import java.util.Map;

public class Chat {
    private String chatId;
    private List<String> participants;
    private String lastMessage;
    private long lastMessageTime;
    private Map<String, String> participantNames; // Map userId -> name
    private String garageId;

    public Chat() {
        // Required for Firestore
    }

    public Chat(String chatId, List<String> participants, String lastMessage, long lastMessageTime,
            Map<String, String> participantNames, String garageId) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.participantNames = participantNames;
        this.garageId = garageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Map<String, String> getParticipantNames() {
        return participantNames;
    }

    public void setParticipantNames(Map<String, String> participantNames) {
        this.participantNames = participantNames;
    }

    public String getGarageId() {
        return garageId;
    }

    public void setGarageId(String garageId) {
        this.garageId = garageId;
    }
}
