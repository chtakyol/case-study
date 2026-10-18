# Spec Delta

## Purpose

Presents the stored content document to the user — its pages and items in a scrollable list, with
selectable choices, and a full-screen image viewer — using the app's Material theme and typography.

## ADDED Requirements

### Requirement: Render the content document in a scrollable list

The system SHALL present every stored page in one scrollable list, in document order, showing each
page's title as a header followed by that page's items, with section titles shown as sub-headers
before their nested items.

#### Scenario: All pages and item types in order

- **WHEN** the stored document contains multiple pages with text, image, and choice items, including
  nested sections
- **THEN** every page and item is shown exactly once, in document order, within a single scrollable
  list

#### Scenario: Nested section

- **WHEN** an item is a section containing nested items
- **THEN** the section title is shown as a sub-header and its nested items follow it

#### Scenario: No stored content

- **WHEN** no content is stored
- **THEN** the screen shows an empty content area

### Requirement: Choose responses for a choice item

The system SHALL present each choice item's responses as selectable chips showing a selected or
unselected state, SHALL indicate whether the item accepts a single response or multiple responses, and
SHALL display the currently selected responses on their own line beneath the item's choices.

#### Scenario: Single selection

- **WHEN** a choice item accepts a single response and the user selects a chip
- **THEN** that chip shows as selected and no other chip in that item is selected

#### Scenario: Multiple selection

- **WHEN** a choice item accepts multiple responses and the user selects a chip
- **THEN** that chip is added to the item's selection without clearing the others

#### Scenario: Deselect

- **WHEN** the user taps a chip that is already selected
- **THEN** that response is removed from the item's selection

#### Scenario: Selection mode is shown

- **WHEN** a choice item is displayed
- **THEN** the screen indicates whether it accepts a single response or multiple responses

#### Scenario: Selected responses shown on a new line

- **WHEN** one or more responses are selected for a choice item
- **THEN** their labels are shown on their own line beneath the item's chips

#### Scenario: Nothing selected

- **WHEN** no response is selected for a choice item
- **THEN** no selected-responses line is shown

### Requirement: Observe content locally and refresh on first load

The system SHALL display content from local storage as the single source of truth and SHALL request a
network refresh when the page content screen first loads.

#### Scenario: First load

- **WHEN** the page content screen is first shown
- **THEN** a refresh is requested and the list renders the locally stored content

#### Scenario: Stored content changes

- **WHEN** the locally stored content changes
- **THEN** the displayed list updates while the screen stays open

#### Scenario: Refresh failure

- **WHEN** the refresh fails
- **THEN** previously stored content remains displayed and a failure message is shown

#### Scenario: First load fails with nothing stored

- **WHEN** the first load fails and nothing is stored
- **THEN** the screen shows a failure message

### Requirement: Load images from their source

The system SHALL load each image item's picture from its source URL and SHALL show a placeholder while
loading and a fallback when the image cannot be loaded.

#### Scenario: Image loads

- **WHEN** an image item has a reachable source
- **THEN** the picture is shown in its row

#### Scenario: Image fails to load

- **WHEN** the source cannot be loaded
- **THEN** a fallback is shown in place of the picture

### Requirement: Open an image in a full-screen viewer

The system SHALL open a full-screen viewer for the tapped image showing that image and its title, and
SHALL provide a way to return to the page content screen.

#### Scenario: Tap an image

- **WHEN** the user taps an image in the list
- **THEN** the image viewer opens showing that image and its title

#### Scenario: Return to the page

- **WHEN** the user navigates back from the viewer
- **THEN** the page content screen is shown again

### Requirement: Theme screens with Material colors, typography, and a top app bar

The system SHALL render content screens with the app's Material theme, SHALL use the theme's typography
styles for titles and body content, and SHALL show a top app bar carrying the relevant title.

#### Scenario: Themed rendering

- **WHEN** a content screen is displayed
- **THEN** colors and typography come from the Material theme, with titles and body text using the
  defined styles

#### Scenario: App bar title

- **WHEN** the page content screen is displayed
- **THEN** its top app bar shows the app title

#### Scenario: Back from the viewer

- **WHEN** the image viewer is displayed
- **THEN** its top app bar offers a back affordance that returns to the page content screen

### Requirement: Host screens in one navigation graph

The system SHALL host the content screens in a single navigation host, and SHALL pass the tapped
image's source and title to the viewer through navigation arguments rather than shared state.

#### Scenario: Image route arguments

- **WHEN** the user taps an image
- **THEN** navigation opens the viewer carrying that image's source and title
