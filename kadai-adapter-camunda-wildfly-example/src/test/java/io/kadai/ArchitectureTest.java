package io.kadai;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.OnlyIncludeTests;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;

class ArchitectureTest {

  public static final String[] PACKAGE_NAMES = {"io.kadai", "acceptance"};
  private static final JavaClasses IMPORTED_CLASSES =
      new ClassFileImporter().importPackages(PACKAGE_NAMES);

  private static final JavaClasses IMPORTED_TEST_CLASSES =
      new ClassFileImporter(List.of(new OnlyIncludeTests())).importPackages(PACKAGE_NAMES);

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
        .haveNameMatching("^should_[A-Z][^_]+(_(For|When)_[A-Z][^_]+)?$")
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void testClassesAndTestMethodsShouldBePackagePrivate() {
    classes()
        .that()
        .haveSimpleNameStartingWith("Test")
        .or()
        .haveSimpleNameEndingWith("Test")
        .should()
        .bePackagePrivate()
        .check(IMPORTED_TEST_CLASSES);
    methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Test")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Test")
        .should()
        .bePackagePrivate()
        .orShould()
        .bePrivate()
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void noMethodsShouldUsePrintln() {
    noClasses().should().callMethodWhere(target(nameMatching("println"))).check(IMPORTED_CLASSES);
  }

  @Test
  void noMethodsShouldUseThreadSleep() {
    noClasses()
        .should()
        .callMethod(Thread.class, "sleep", long.class)
        .orShould()
        .callMethod(Thread.class, "sleep", long.class, int.class)
        .check(IMPORTED_CLASSES);
  }

  @Test
  void noImportsForOldJunitClasses() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.Test")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.Assert")
        .check(IMPORTED_TEST_CLASSES);
  }

  @Test
  void noImportsForJunitAssertionsWeWantAssertJ() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.jupiter.api.Assertions")
        .check(IMPORTED_TEST_CLASSES);
  }
}
