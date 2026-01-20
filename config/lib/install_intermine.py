import argparse
import glob
import os
from pathlib import Path
from subprocess import run
import sys

EXIT_SUCCESS = 0
EXIT_FAILURE = 1
EXIT_USER = 2


class IntermineBuilder:
    def __init__(self, clean: bool = False) -> None:
        python_lib_dir = os.path.dirname(os.path.realpath(__file__))
        self.clean = clean
        self.checkout = os.path.join(python_lib_dir, "..", "..")
        self.psql_host = self.getenv("PSQL_HOST")
        self.psql_user = self.getenv("PSQL_USER")
        self.psql_pwd = self.getenv("PSQL_PWD")

    def getenv(self, name: str) -> str:
        value = os.getenv(name)
        if value is None:
            print(f"You must set the environment variable {name}")
            sys.exit(EXIT_USER)

        return value

    def build(self) -> None:
        self.create_gradle_properties()
        self.copy_test_properties()
        self.gradle_install_modules()

    def create_gradle_properties(self) -> None:
        for gradle_properties_in in glob.glob(
            "**/gradle.properties.in", root_dir=self.checkout, recursive=True
        ):
            in_file = os.path.join(self.checkout, gradle_properties_in)
            out_file = in_file[:-3]
            with open(out_file, "w") as f_out:
                f_out.write(
                    "# FILE AUTOMATICALLY GENERATED FROM "
                    f"{gradle_properties_in}. DO NOT EDIT!\n"
                )
                with open(in_file) as f_in:
                    for line in f_in:
                        f_out.write(
                            line.replace("@@im_checkout@@", self.checkout)
                        )

            print(f"Written {out_file}")

    def copy_test_properties(self) -> None:
        self.copy_properties("ci.properties", "intermine-test.properties")
        self.copy_properties("testmodel.properties", "testmodel.properties")
        self.copy_properties(
            "ci-bio.properties", "intermine-bio-test.properties"
        )

    def copy_properties(self, source_name: str, dest_name: str) -> None:
        dot_intermine_dir = os.path.join(Path.home(), ".intermine")
        os.makedirs(dot_intermine_dir, exist_ok=True)

        source_file = os.path.join(self.checkout, "config", source_name)
        dest_file = os.path.join(dot_intermine_dir, dest_name)

        if os.path.exists(dest_file):
            print(f"Skipping {dest_file} as it already exists")
            return

        with open(dest_file, "w") as f_out:
            f_out.write(
                "# FILE AUTOMATICALLY GENERATED FROM "
                f"{source_file}. DO NOT EDIT!\n"
            )
            with open(source_file) as f_in:
                for line in f_in:
                    line = line.replace("PSQL_HOST", self.psql_host)
                    line = line.replace("PSQL_USER", self.psql_user)
                    line = line.replace("PSQL_PWD", self.psql_pwd)

                    f_out.write(line)
        print(f"Written {dest_file}")

    def gradle_install_modules(self, clean=False) -> None:
        modules = [
            "plugin",
            "intermine",
            "bio",
            "bio/sources",
            "bio/postprocess",
        ]

        for module in modules:
            self.gradle_install(module)

    def gradle_install(self, module: str) -> None:
        module_dir = os.path.join(self.checkout, module)
        os.chdir(module_dir)
        if self.clean:
            run(["./gradlew", "clean", "--stacktrace"], check=True)
        run(["./gradlew", "install", "--stacktrace"], check=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Install Intermine")
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Run gradlew --clean before install",
    )

    args = parser.parse_args()

    builder = IntermineBuilder(clean=args.clean)
    builder.build()


if __name__ == "__main__":
    main()
