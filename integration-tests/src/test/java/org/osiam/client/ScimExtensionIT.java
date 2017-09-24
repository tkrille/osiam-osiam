/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.client;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osiam.client.oauth.Scope;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryBuilder;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.Extension.Field;
import org.osiam.resources.scim.ExtensionFieldType;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
@DatabaseTearDown(value = "/database_tear_down.xml", type = DatabaseOperation.DELETE_ALL)
public class ScimExtensionIT extends AbstractIntegrationTestBase {

    private static final String URN = "extension";
    private static final String EXISTING_USER_UUID = "df7d06b2-b6ee-42b1-8c1b-4bd1176cc8d4";

    private Map<String, Extension.Field> extensionData;
    private Map<String, Extension.Field> extensionDataToPatch;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() {
        extensionData = new HashMap<>();
        extensionData.put("gender", new Extension.Field(ExtensionFieldType.STRING, "male"));
        extensionData.put("newsletter", new Extension.Field(ExtensionFieldType.BOOLEAN, "true"));
        extensionData.put("age", new Extension.Field(ExtensionFieldType.INTEGER, "28"));
        extensionData.put("weight", new Extension.Field(ExtensionFieldType.DECIMAL, "82.7"));
        extensionData.put("birthday", new Extension.Field(ExtensionFieldType.DATE_TIME, "2008-01-23T04:56:22.000Z"));
        extensionData.put("photo", new Extension.Field(ExtensionFieldType.BINARY, "ZXhhbXBsZQ=="));
        extensionData.put("mother", new Extension.Field(ExtensionFieldType.REFERENCE, "https://example.com/Users/28"));

        extensionDataToPatch = new HashMap<>();
        extensionDataToPatch.put("gender", new Extension.Field(ExtensionFieldType.STRING, "female"));
        extensionDataToPatch.put("newsletter", new Extension.Field(ExtensionFieldType.BOOLEAN, "false"));
        extensionDataToPatch.put("age", new Extension.Field(ExtensionFieldType.INTEGER, "32"));
        extensionDataToPatch.put("weight", new Extension.Field(ExtensionFieldType.DECIMAL, "78.7"));
        extensionDataToPatch.put("birthday", new Extension.Field(ExtensionFieldType.DATE_TIME,
                "2005-01-23T04:56:22.000Z"));
        extensionDataToPatch.put("photo", new Extension.Field(ExtensionFieldType.BINARY, "Y2hhbmdlZA=="));
        extensionDataToPatch.put("mother", new Extension.Field(ExtensionFieldType.REFERENCE,
                "https://www.example.com/Users/99"));
        accessToken = OSIAM_CONNECTOR.retrieveAccessToken("marissa", "koala", Scope.ADMIN);
    }

    @Test
    @DatabaseSetup(value = "/database_seeds/ScimExtensionIT/extensions.xml")
    public void retrieving_a_user_with_extension_works() {
        User storedUser = OSIAM_CONNECTOR.getUser(EXISTING_USER_UUID, accessToken);

        assertTrue(storedUser.getSchemas().contains(URN));
        Extension storedExtension = storedUser.getExtension(URN);
        assertExtensionEqualsExtensionMap(storedExtension, extensionData);
    }

    @Test
    @DatabaseSetup("/database_seeds/ScimExtensionIT/extensions_and_multiple_users.xml")
    public void retrieving_multiple_users_with_extension_via_query_works() {
        Query query = new QueryBuilder().filter("userName co \"existing\"").build();

        SCIMSearchResult<User> result = OSIAM_CONNECTOR.searchUsers(query, accessToken);

        assertThat(result.getResources().size(), is(3));
        for (User storedUser : result.getResources()) {
            assertTrue(storedUser.getSchemas().contains(URN));
            Extension storedExtension = storedUser.getExtension(URN);
            for (Map.Entry<String, Extension.Field> entry : extensionData.entrySet()) {
                String fieldName = entry.getKey();
                Extension.Field expectedField = entry.getValue();
                ExtensionFieldType<?> expectedType = expectedField.getType();
                Object expectedValue = expectedType.fromString(expectedField.getValue());
                Object actualValue = storedExtension.getField(fieldName, expectedType);

                assertEquals(expectedValue, actualValue);
            }
        }
    }

    @Test
    @DatabaseSetup(value = "/database_seeds/ScimExtensionIT/add_user.xml")
    public void adding_a_user_with_extension_data_to_database_works() {
        Extension extension = createExtensionWithData(URN, extensionData);
        User user = new User.Builder("userName").setPassword("password").addExtension(extension).build();

        String uuid = OSIAM_CONNECTOR.createUser(user, accessToken).getId();

        User storedUser = OSIAM_CONNECTOR.getUser(uuid, accessToken);
        assertTrue(storedUser.getSchemas().contains(URN));
        assertExtensionEqualsExtensionMap(storedUser.getExtension(URN), extensionData);
    }

    @Test
    @DatabaseSetup(value = "/database_seeds/ScimExtensionIT/extensions.xml")
    public void replacing_a_user_with_extension_data_to_database_works() {
        User existingUser = OSIAM_CONNECTOR.getUser(EXISTING_USER_UUID, accessToken);
        extensionData.put("gender", new Extension.Field(ExtensionFieldType.STRING, "female"));
        Extension extension = existingUser.getExtension(URN);
        Extension newExtension = new Extension.Builder(extension).setField("gender", "female").build();

        User replaceUser = new User.Builder(existingUser).addExtension(newExtension).build();

        OSIAM_CONNECTOR.replaceUser(EXISTING_USER_UUID, replaceUser, accessToken);

        User storedUser = OSIAM_CONNECTOR.getUser(EXISTING_USER_UUID, accessToken);
        Extension storedExtension = storedUser.getExtension(URN);

        assertExtensionEqualsExtensionMap(storedExtension, extensionData);
    }

    @Test
    @DatabaseSetup(value = "/database_seeds/ScimExtensionIT/extensions.xml")
    public void deleting_a_user_also_deletes_her_extension_values() throws SQLException {
        OSIAM_CONNECTOR.deleteUser(EXISTING_USER_UUID, accessToken);

        String sql = "SELECT count(*) FROM scim_extension_field_value WHERE user_internal_id = 2";
        ResultSet rs = dataSource.getConnection().createStatement().executeQuery(sql);
        rs.next();

        assertThat(rs.getInt(1), is(0));
    }

    private Extension createExtensionWithData(String urn, Map<String, Extension.Field> extensionData) {
        Extension.Builder extensionBuilder = new Extension.Builder(urn);

        for (Entry<String, Extension.Field> fieldData : extensionData.entrySet()) {
            Extension.Field field = fieldData.getValue();
            ExtensionFieldType<?> type = field.getType();
            String value = field.getValue();

            addOrUpdateExtension(extensionBuilder, fieldData.getKey(), value, type);
        }

        return extensionBuilder.build();
    }

    private <T> void addOrUpdateExtension(Extension.Builder extensionBuilder, String fieldName, String value,
            ExtensionFieldType<T> type) {
        extensionBuilder.setField(fieldName, type.fromString(value), type);
    }

    private void assertExtensionEqualsExtensionMap(Extension storedExtension, Map<String, Field> extensionMap) {
        assertThat(storedExtension.getFields().size(), is(extensionMap.size()));

        for (Map.Entry<String, Extension.Field> entry : extensionMap.entrySet()) {
            String fieldName = entry.getKey();
            Extension.Field expectedField = entry.getValue();
            ExtensionFieldType<?> expectedType = expectedField.getType();
            Object expectedValue = expectedType.fromString(expectedField.getValue());
            Object actualValue = storedExtension.getField(fieldName, expectedType);

            assertEquals(expectedValue, actualValue);
        }
    }
}
