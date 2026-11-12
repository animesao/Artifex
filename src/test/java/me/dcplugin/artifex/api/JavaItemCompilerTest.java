package me.dcplugin.artifex.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты чистой логики Java-компилятора:
 * генерация имени класса и вставка импортов.
 */
class JavaItemCompilerTest {

    @Test
    void simpleFileNameBecomesClassName() {
        assertEquals("messagesItem", JavaItemCompiler.sanitizeGeneratedClassName("messages.java"));
    }

    @Test
    void dashesAndSpacesBecomeUnderscores() {
        assertEquals("my_itemItem", JavaItemCompiler.sanitizeGeneratedClassName("my-item.java"));
        assertEquals("my_itemItem", JavaItemCompiler.sanitizeGeneratedClassName("my item.java"));
    }

    @Test
    void nestedPathBecomesFlatName() {
        assertEquals("shop_shopItem", JavaItemCompiler.sanitizeGeneratedClassName("shop/shop.java"));
        assertEquals("shop_shopItem", JavaItemCompiler.sanitizeGeneratedClassName("shop\\shop.java"));
    }

    /**
     * Имя класса должно быть корректным Java-идентификатором:
     * точка или цифра в начале делали исходник некомпилируемым.
     */
    @Test
    void generatedNameIsAlwaysValidIdentifier() {
        String name = JavaItemCompiler.sanitizeGeneratedClassName("my.item.java");
        assertTrue(Character.isJavaIdentifierStart(name.charAt(0)), name);
        for (int i = 0; i < name.length(); i++) {
            assertTrue(Character.isJavaIdentifierPart(name.charAt(i)), name);
        }
    }

    @Test
    void nameStartingWithDigitIsPrefixed() {
        String name = JavaItemCompiler.sanitizeGeneratedClassName("1abc.java");
        assertTrue(Character.isJavaIdentifierStart(name.charAt(0)), name);
        assertTrue(name.startsWith("_"), name);
    }

    @Test
    void importsAreInsertedAtTheBeginning() {
        String source = "public class Foo {}\n";
        String result = JavaItemCompiler.insertImports(source, List.of("a.B"));
        assertTrue(result.startsWith("import a.B;\n"), result);
    }

    /**
     * package должен остаться первым оператором — иначе javac откажется
     * собирать файл.
     */
    @Test
    void importsAreInsertedAfterPackageDeclaration() {
        String source = "package my.pkg;\n\npublic class Foo {}\n";
        String result = JavaItemCompiler.insertImports(source, List.of("a.B"));
        assertTrue(result.startsWith("package my.pkg;\n"), result);
        assertTrue(result.contains("import a.B;"), result);
        assertTrue(result.indexOf("import a.B;") > result.indexOf("package my.pkg;"), result);
    }

    @Test
    void emptyImportListKeepsSourceUnchanged() {
        String source = "public class Foo {}\n";
        assertEquals(source, JavaItemCompiler.insertImports(source, List.of()));
        assertEquals(source, JavaItemCompiler.insertImports(source, null));
    }

    @Test
    void packageDeclarationIsStripped() {
        assertEquals("public class Foo {}\n",
                JavaItemCompiler.stripPackageDeclaration("package my.pkg;\npublic class Foo {}\n"));
    }

    @Test
    void sourceWithoutPackageIsUnchangedByStrip() {
        String source = "public class Foo {}\n";
        assertEquals(source, JavaItemCompiler.stripPackageDeclaration(source));
    }

    @Test
    void legacyPackageImportsAreMigrated() {
        String legacy = "import me.dcplugin.dcustomitems.api.AbstractCustomItem;\n";
        String expected = "import me.dcplugin.artifex.api.AbstractCustomItem;\n";
        assertEquals(expected, JavaItemCompiler.migrateLegacyImports(legacy));
    }

    @Test
    void dangerousTokensAreDetected() {
        assertNull(JavaItemCompiler.findDangerousToken("public class SafeItem extends AbstractCustomItem {}"));
        assertEquals("System.exit", JavaItemCompiler.findDangerousToken("System.exit(0);"));
        assertEquals("ProcessBuilder", JavaItemCompiler.findDangerousToken("new ProcessBuilder(\"calc\").start();"));
        assertEquals("Runtime.getRuntime().exec", JavaItemCompiler.findDangerousToken("Runtime.getRuntime().exec(\"ls\");"));
        assertEquals(".setAccessible(", JavaItemCompiler.findDangerousToken("field.setAccessible(true);"));
    }
}
