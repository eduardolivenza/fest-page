package com.festdescans.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The hexagon's dependency rule, checked on the production classes. */
@AnalyzeClasses(packages = "com.festdescans.api", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    /** domain <- application <- adapters; adapters never depend on each other. */
    @ArchTest
    static final ArchRule layers = onionArchitecture()
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("turnstile", "..adapter.out.turnstile..")
            .adapter("password", "..adapter.out.password..")
            .withOptionalLayers(true)
            // The composition root (FestPageApplication, UseCaseConfiguration) wires all layers together.
            .ignoreDependency(resideInAPackage("com.festdescans.api"), alwaysTrue());

    /** The core is plain Java: no Spring, JPA, Hibernate or Jackson. */
    @ArchTest
    static final ArchRule frameworkFreeCore = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..", "com.fasterxml..");
}
