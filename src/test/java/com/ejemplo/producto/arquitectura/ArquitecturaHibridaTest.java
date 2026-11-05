package com.ejemplo.producto.arquitectura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Verifica automáticamente que el código respete las dos arquitecturas combinadas.
 * Si alguien rompe una regla (por ejemplo, el controlador usa un caso de uso concreto),
 * esta prueba falla.
 */
class ArquitecturaHibridaTest {

    private static final String BASE = "com.ejemplo.producto";

    private static JavaClasses clases;

    @BeforeAll
    static void importar() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    /** Cebolla: cada anillo solo conoce a los que están más adentro. */
    @Test
    void respetaLosAnillosDeLaCebolla() {
        onionArchitecture()
                .domainModels(BASE + ".domain.model..", BASE + ".domain.exception..")
                .domainServices(BASE + ".domain.ports..")
                .applicationServices(BASE + ".application..")
                .adapter("web", BASE + ".infrastructure.adapters.in..")
                .adapter("persistencia", BASE + ".infrastructure.adapters.out..")
                .adapter("config", BASE + ".infrastructure.config..")
                // config es el ensamblador: es el único que puede conocer a todos
                .ignoreDependency(
                        DescribedPredicate.describe("config",
                                c -> c.getPackageName().startsWith(BASE + ".infrastructure.config")),
                        DescribedPredicate.alwaysTrue())
                .check(clases);
    }

    /** Hexagonal: los adaptadores de entrada solo hablan con puertos de entrada, nunca con casos de uso concretos. */
    @Test
    void adaptadoresDeEntradaUsanSoloPuertos() {
        noClasses().that().resideInAPackage(BASE + ".infrastructure.adapters.in..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".application.usecase..")
                .check(clases);
    }

    /** Hexagonal: los casos de uso solo conocen puertos de salida, no adaptadores. */
    @Test
    void casosDeUsoNoConocenAdaptadores() {
        noClasses().that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure..")
                .check(clases);
    }

    /** El dominio y la aplicación son Java puro: sin Spring, JPA ni HTTP. */
    @Test
    void nucleoSinFrameworks() {
        noClasses().that().resideInAnyPackage(BASE + ".domain..", BASE + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "jakarta.servlet..")
                .check(clases);
    }
}
