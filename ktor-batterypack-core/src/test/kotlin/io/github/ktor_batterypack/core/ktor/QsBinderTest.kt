package io.github.ktor_batterypack.core.ktor

import io.ktor.http.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class QsBinderTest {

    private val mapper = JsonMapper()

    @Test
    fun `should convert dot-separated keys into nested objects`() {
        val params = Parameters.build {
            append("prop1.prop2.prop3", "5")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"prop1":{"prop2":{"prop3":"5"}}}""")
        )
    }

    @Test
    fun `should convert indexed keys into arrays of objects`() {
        val params = Parameters.build {
            append("prop1.0.prop2", "5")
            append("prop1.1.prop2", "10")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"prop1":[{"prop2":"5"},{"prop2":"10"}]}""")
        )
    }

    @Test
    fun `should convert multiple values for the same key into an array`() {
        val params = Parameters.build {
            append("prop1", "5")
            append("prop1", "10")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"prop1":["5","10"]}""")
        )
    }

    @Test
    fun `should return empty object for empty params`() {
        val result = QsBinder.convert(Parameters.Empty)

        assertThat(result).isEqualTo(mapper.readTree("""{}"""))
    }

    @Test
    fun `should convert flat key without dots into top-level property`() {
        val params = Parameters.build {
            append("name", "John")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"name":"John"}"""))
    }

    @Test
    fun `should convert flat and nested keys together`() {
        val params = Parameters.build {
            append("name", "John")
            append("address.city", "Warsaw")
            append("address.zip", "00-001")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"name":"John","address":{"city":"Warsaw","zip":"00-001"}}""")
        )
    }

    @Test
    fun `should convert indexed keys into arrays of scalars`() {
        val params = Parameters.build {
            append("tags.0", "red")
            append("tags.1", "green")
            append("tags.2", "blue")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"tags":["red","green","blue"]}"""))
    }

    @Test
    fun `should place indexed values at correct positions regardless of input order`() {
        val params = Parameters.build {
            append("items.1", "b")
            append("items.0", "a")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"items":["a","b"]}"""))
    }

    @Test
    fun `should convert multiple values for the same nested key into an array`() {
        val params = Parameters.build {
            append("a.b", "5")
            append("a.b", "10")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"a":{"b":["5","10"]}}"""))
    }

    @Test
    fun `should convert multiple values for the same indexed element into nested array`() {
        val params = Parameters.build {
            append("tags.0", "a")
            append("tags.0", "b")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"tags":[["a","b"]]}"""))
    }

    @Test
    fun `should convert indexed keys into arrays of objects with multiple properties`() {
        val params = Parameters.build {
            append("items.0.name", "apple")
            append("items.0.qty", "5")
            append("items.1.name", "banana")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"items":[{"name":"apple","qty":"5"},{"name":"banana"}]}""")
        )
    }

    @Test
    fun `should bind deeply nested keys regardless of input order`() {
        val params = Parameters.build {
            append("a.b.c.d", "deep")
            append("a.b.e", "f")
            append("a.x", "1")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(
            mapper.readTree("""{"a":{"x":"1","b":{"e":"f","c":{"d":"deep"}}}}""")
        )
    }

    @Test
    fun `should treat numeric-looking but non-integer segments as object properties`() {
        val params = Parameters.build {
            append("prop.1a", "v")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"prop":{"1a":"v"}}"""))
    }

    @Test
    fun `should not merge keys sharing a common prefix across dot boundaries`() {
        val params = Parameters.build {
            append("a.b", "1")
            append("ab.c", "2")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"a":{"b":"1"},"ab":{"c":"2"}}"""))
    }

    @Test
    fun `should fill missing indices with nulls for sparse arrays`() {
        val params = Parameters.build {
            append("items.2", "x")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"items":[null,null,"x"]}"""))
    }

    @Test
    fun `should throw when root-level key is numeric`() {
        val params = Parameters.build {
            append("0", "a")
        }

        assertThatThrownBy { QsBinder.convert(params) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should throw when root segment of a nested key is numeric`() {
        val params = Parameters.build {
            append("0.name", "a")
        }

        assertThatThrownBy { QsBinder.convert(params) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should keep empty values as empty strings`() {
        val params = Parameters.build {
            append("a", "")
            append("b.c", "")
        }

        val result = QsBinder.convert(params)

        assertThat(result).isEqualTo(mapper.readTree("""{"a":"","b":{"c":""}}"""))
    }

}
