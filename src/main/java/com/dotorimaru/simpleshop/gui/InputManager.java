package com.dotorimaru.simpleshop.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 채팅 입력 대기 상태 관리. "가격 입력" 등에 사용.
 * 입력을 무시하고 방치한 경우 일정 시간이 지나면 자동 만료되어
 * 이후의 일반 채팅이 가격으로 오인되지 않도록 한다.
 */
public class InputManager {
    private record Pending(Consumer<String> callback, long expiresAt) {}

    private final Map<UUID, Pending> waiting = new ConcurrentHashMap<>();
    private final long timeoutMillis;

    public InputManager(long timeoutSeconds) {
        this.timeoutMillis = Math.max(5, timeoutSeconds) * 1000L;
    }

    public void await(Player p, Consumer<String> onInput) {
        waiting.put(p.getUniqueId(), new Pending(onInput, System.currentTimeMillis() + timeoutMillis));
    }

    public boolean isWaiting(UUID uuid) {
        Pending pending = waiting.get(uuid);
        if (pending == null) return false;
        if (System.currentTimeMillis() > pending.expiresAt()) { // 만료된 입력은 폐기
            waiting.remove(uuid);
            return false;
        }
        return true;
    }

    /** 입력 소비. 콜백 반환. 없거나 만료면 null. */
    public Consumer<String> consume(UUID uuid) {
        Pending pending = waiting.remove(uuid);
        if (pending == null || System.currentTimeMillis() > pending.expiresAt()) return null;
        return pending.callback();
    }

    public void cancel(UUID uuid) { waiting.remove(uuid); }
}
