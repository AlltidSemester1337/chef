---
name: figma
description: Use whenever there is a need to access UX design related matters.
---

Figma
The Figma design for the app can be found on the local MCP server.
We are using designs from the file Chef UI and the page Final UI. To the left on that page, you can find the base components that build bigger components. On the same page, there are screens laying out the flow for the signup and onboarding. There are also screens specifying the main screens including homepage, recipe page, collections screen, and chat screen.

When implementing UI:
1. Identify the closest Material component
2. Implement using Material3: https://m3.material.io/
3. Apply styling via theme
4. Only deviate if no equivalent exists

## Design Philosophy

- Always implement UI using Jetpack Compose Material3 components.
- Do NOT recreate custom components if a Material equivalent exists.
- Figma is a visual reference, not a source of truth for structure.
- Prioritize accessibility, usability, and platform conventions over pixel-perfect fidelity.

## Component Mapping

Map Figma components to Material3 equivalents:

- Buttons → Button / FilledTonalButton / OutlinedButton
- Inputs → TextField / OutlinedTextField
- Cards → Card
- Lists → LazyColumn + ListItem
- Dialogs → AlertDialog / ModalBottomSheet

Avoid:
- Custom touch handling when Material component exists
- Hardcoded sizes/colors

## Design Tokens

Extract tokens from Figma and define them in theme:

### Colors
- Primary: #XXXXXX
- Secondary: #XXXXXX
- Background: #XXXXXX

### Typography
- HeadingLarge
- BodyMedium
- LabelSmall

### Shapes
- Small: 8dp
- Medium: 12dp
- Large: 16dp

### Spacing
- 4dp scale system (4, 8, 12, 16, 24, 32)

## Styling Rules

- Apply Figma styling via theming, not inline overrides.
- Use Modifier.padding instead of fixed layout sizes.
- Maintain minimum touch target size of 48dp.
- Respect Material accessibility contrast ratios.

## Constraints

- Never generate pixel-perfect layouts that break Material guidelines.
- Never use absolute positioning unless strictly necessary.
- Prefer composition over custom drawing.
- All UI must be responsive.

## Accessibility

- All interactive elements must have contentDescription.
- Ensure sufficient color contrast.
- Use semantic roles where applicable.
