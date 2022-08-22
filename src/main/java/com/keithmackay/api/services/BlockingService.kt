package com.keithmackay.api.services

import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.Cacher
import com.keithmackay.api.utils.doc
import lombok.extern.slf4j.Slf4j
import org.bson.Document
import java.time.Duration
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton

@Slf4j
@Singleton
class BlockingService @Inject internal constructor(private val db: Database) {
    private val cache: Cacher<Set<String>> = Cacher(Duration.ofMinutes(15), "LS Block Hosts")

    fun getAllDomains(): Set<String> {
        return cache.get("all") {
            db.getLsCollection()
                    .find()
                    .projection(doc("server", 1))
                    .into(ArrayList())
                    .stream()
                    .map { it: Document -> it.getString("server") }
                    .collect(Collectors.toSet())
        }
    }
}