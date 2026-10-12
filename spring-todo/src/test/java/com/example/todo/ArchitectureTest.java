package com.example.todo;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@AnalyzeClasses(packages = "com.example.todo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // No Function layer yet: the app is not event-driven. Add it with the first Spring Cloud Function bean.
    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller")
            .definedBy("..controller..")
            .layer("Service")
            .definedBy("..service..")
            .layer("Repository")
            .definedBy("..repository..")
            .whereLayer("Controller")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository")
            .mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule noCycles =
            slices().matching("com.example.todo.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule noFieldInjection = noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule noValueInjection = noClasses()
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Value.class)
            .because("settings bind to @ConfigurationProperties records");

    @ArchTest
    static final ArchRule controllersNeverSeeEntities = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..entity..");

    @ArchTest
    static final ArchRule transactionsBelongToServices = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Transactional.class);

    @ArchTest
    static final ArchRule dtosAreRecords =
            classes().that().resideInAPackage("..dto..").should().beAssignableTo(Record.class);

    // Without @Validated, constraint annotations on service parameters are silently ignored
    @ArchTest
    static final ArchRule servicesValidateTheirParameters =
            classes().that().areAnnotatedWith(Service.class).should().beAnnotatedWith(Validated.class);
}
