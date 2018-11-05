/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.scala

import org.gradle.integtests.fixtures.ScalaCoverage
import org.gradle.language.scala.internal.toolchain.DefaultScalaToolProvider
import org.gradle.test.fixtures.file.TestFile

import static org.gradle.integtests.fixtures.RepoScriptBlockUtil.mavenCentralRepository

class ScalaCompilationFixture {
    private final TestFile root
    final ScalaClass basicClassSource
    final ScalaClass classDependingOnBasicClassSource
    final ScalaClass independentClassSource
    final String sourceSet
    String scalaVersion
    String zincVersion
    String sourceCompatibility
    String sourceDir
    TestFile analysisFile

    ScalaCompilationFixture(File root) {
        this.root = new TestFile(root)
        this.analysisFile = this.root.file("build/tmp/scala/compilerAnalysis/compileScala.analysis")
        this.sourceSet = 'main'
        this.sourceDir = 'src/main/scala'
        this.scalaVersion = ScalaCoverage.NEWEST
        this.zincVersion = DefaultScalaToolProvider.DEFAULT_ZINC_VERSION
        this.sourceCompatibility = '1.7'
        basicClassSource = new ScalaClass(
            'Person',
            '''
                /**
                 * A person.
                 * Can live in a house.
                 * Has a name and an age.
                 */
                class Person(val name: String, val age: Int)'''.stripIndent(),
            '''                 
                /**
                 * A person.
                 * Can live in a house.
                 * Has a name, age and a height.
                 */
                class Person(val name: String, val age: Int, val height: Int)'''.stripIndent())
        classDependingOnBasicClassSource = new ScalaClass(
            'House',
            'class House(val owner: Person)',
            'class House(val owner: Person, val residents: List[Person])'
        )
        independentClassSource = new ScalaClass(
            'Other',
            'class Other',
            'class Other(val some: String)'
        )
    }

    def buildScript() {
        return """
            apply plugin: 'scala'
                        
            ${mavenCentralRepository()}

            dependencies {
                zinc "org.scala-sbt:zinc_2.12:${zincVersion}"
                compile "org.scala-lang:scala-library:${scalaVersion}" 
            }
            
            sourceSets {
                main {
                    scala {
                        srcDirs = ['${sourceDir}']
                    }
                }
            }

            sourceCompatibility = '${sourceCompatibility}'
            targetCompatibility = '${sourceCompatibility}'
        """.stripIndent()
    }

    void baseline() {
        basicClassSource.create()
        classDependingOnBasicClassSource.create()
        independentClassSource.create()
    }

    List<ScalaClass> getAll() {
        return [basicClassSource, classDependingOnBasicClassSource, independentClassSource]
    }

    List<Long> getAllClassesLastModified() {
        return all*.compiledClass*.lastModified()
    }

    class ScalaClass {
        final TestFile source
        final TestFile compiledClass
        final String originalText
        final String changedText
        final String javadocLocation

        ScalaClass(String path, String originalText, String changedText) {
            this.changedText = changedText
            this.originalText = originalText
            source = root.file("${sourceDir}/${path}.scala")
            compiledClass = root.file("build/classes/scala/main/${path}.class")
            javadocLocation = root.file("build/docs/scaladoc/${path}.html")
        }

        void create() {
            source.text = originalText
        }

        void change() {
            source.text = changedText
        }
    }

}
