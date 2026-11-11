package com.festdescans.api.adapter.out.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/** Only the persistence adapter, against an embedded PostgreSQL. */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(PersistenceConfiguration.class)
class PersistenceTestApplication {
}
