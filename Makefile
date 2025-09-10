# Makefile for Android project

.PHONY: test

test:
	./gradlew test

.PHONY: connectedTest

connectedTest:
	./gradlew connectedAndroidTest

.PHONY: clean

clean:
	./gradlew clean

.PHONY: build

build:
	./gradlew build

.PHONY: alltests

alltests:
	./gradlew test connectedAndroidTest
