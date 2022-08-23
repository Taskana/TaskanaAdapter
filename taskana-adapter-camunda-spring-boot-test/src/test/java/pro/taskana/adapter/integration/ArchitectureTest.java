package pro.taskana.adapter.integration;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;

class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void init() {
    // time intensive operation should only be done once
    importedClasses = new ClassFileImporter().importPackages("pro.taskana", "acceptance");
  }

  @Test
  void testMethodNamesShouldMatchAccordingToOurGuidelines() {

    methods()
        .that(
            are(
                annotatedWith(Test.class)
                    .or(annotatedWith(TestFactory.class))
                    .or(annotatedWith(TestTemplate.class))))
        .and()
        .areNotDeclaredIn(ArchitectureTest.class)
        .should()
        .bePackagePrivate()
        .andShould()
        .haveNameMatching("^should_[A-Z][^_]+(_(For|When)_[A-Z][^_]+)?$")
        .check(importedClasses);
  }
}
