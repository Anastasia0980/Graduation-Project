package org.example.rlplatform.battle;

import org.example.rlplatform.entity.Submission;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 按 environment 分队列：同一环境的两个提交才能匹配对战
 */
@Component
public class InMemoryBattleQueue {

    private final Map<String, ConcurrentLinkedQueue<Submission>> queues = new ConcurrentHashMap<>();

    public int enqueue(Submission submission) {
        String env = normalizeEnv(submission.getEnvironment());
        submission.setEnvironment(env);

        queues.putIfAbsent(env, new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Submission> q = queues.get(env);
        q.add(submission);
        return q.size();
    }

    /**
     * 若队列中 >=2，则弹出两条作为一场对战；否则返回 null
     */
    public Submission[] tryDequeuePair(String environment) {
        String env = normalizeEnv(environment);
        ConcurrentLinkedQueue<Submission> q = queues.get(env);
        if (q == null) return null;

        Submission a = q.poll();
        if (a == null) return null;

        Submission b = q.poll();
        if (b == null) {
            // 没凑够 2 个，把 a 放回去
            q.add(a);
            return null;
        }
        return new Submission[]{a, b};
    }

    private String normalizeEnv(String env) {
        if (env == null || env.isBlank()) return "tictactoe_v3";
        return env.trim();
    }
}