package com.martin;

import java.time.LocalDate;
import java.util.List;

import static org.easymock.EasyMock.*;

public class SomeServiceMockBuilder {
    private String string;
    private int number;
    private List<String> strings;
    private byte[] bytes;

    private SomeServiceMockBuilder() {
    }

    public static SomeServiceMockBuilder aSomeService() {
        return new SomeServiceMockBuilder();
    }

    public SomeServiceMockBuilder withString(String string) {
        this.string = string;
        return this;
    }

    public SomeServiceMockBuilder withNumber(int number) {
        this.number = number;
        return this;
    }

    public SomeServiceMockBuilder withStrings(List<String> strings) {
        this.strings = strings;
        return this;
    }

    public SomeServiceMockBuilder withBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public SomeService build() {
        SomeService someService = createMock(SomeService.class);

        expect(someService.methodWithReturn(isA(LocalDate.class), isA(Number.class))).andStubReturn(string);
        expect(someService.soPrimitive(isA(LocalDate.class), isA(Number.class), anyInt())).andStubReturn(number);
        expect(someService.aaarrraaay(isA(LocalDate.class), isA(Number.class))).andStubReturn(strings);
        expect(someService.bytes(isA(int[].class))).andStubReturn(bytes);

        someService.methodWithoutReturn(anyInt());

        replay(someService);

        return someService;
    }
}
