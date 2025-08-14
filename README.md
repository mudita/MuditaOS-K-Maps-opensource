# License and Acknowledgements:

## Source

This application is based on [OsmAnd](https://github.com/osmandapp/OsmAnd) and is integrated with MapAPI for additional mapping features.

## License

This project is licensed under the terms of the GNU General Public License version 3 [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html).
MapAPI in licensed under the terms of MIT License.

## Credits

This application incorporates and builds upon components of the OsmAnd project.
OsmAnd is © OsmAnd B.V. and contributors, licensed under GPLv3.

# Github flow:
- Two approvals (at least 1 from Mudita side)
- Merge commit has to start with one of the prefixes:
    - `fi:` for bug fixes
    - `ft:` for  features
    - `im:` for improvements
- Each **merge commit** (`develop` <- `feature_branch`) should contain corresponding Jira ticket number and short explanation

  i.e.: `ft:XY-123_board_pieces_movement_implementation`.
- Each branch should contain corresponding Jira ticket number and short explanation

  i.e.: `feature/XY-123_board_pieces_movement_implementation`
- Each PR should contain corresponding Jira ticket number and short explanation

  i.e.: `feature/XY-123 Implemented board pieces movement`

# CI/CD & release

## Release flow:

* version name is bumped, i.e.: 1.2.3
* tag is created based on the build type: development / release, i.e.: development.1.2.3
* CI/CD is triggered

## CI/CD flow

* Version check (if the version name was bumped)
* APK is generated based on the tag prefix - development / release
* Changelog is generated - collects commits with specified prefixes (fi:, ft:, im:)
* Upload to Nexus - APK + Changelog

## Local configuration
- [Local configuration](./android/README.md)
