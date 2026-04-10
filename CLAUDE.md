# CLAUDE.md

This file provides guidance for AI assistants (such as Claude) working in this repository.

## Repository Overview

**hello-world** is a minimal GitHub learning project created by Kuldeep Vaishnav in August 2016. It was used to practice fundamental Git and GitHub workflows — creating a repository, making commits, opening branches, submitting pull requests, and merging changes.

There is no application code in this repository. The sole tracked file is `README.md`.

## Repository Structure

```
hello-world/
└── README.md   # Project title and brief description
```

## Commit History

| Hash     | Message                         | Author               | Date         |
|----------|---------------------------------|----------------------|--------------|
| 402b96e  | Merge pull request #1 from kuldeepvaishnav/readme-edits | kuldeepvaishnav | 2016-08-12 |
| 80631cc  | Update README.md                | kuldeepvaishnav      | 2016-08-12   |
| 2b184cd  | Initial commit                  | kuldeepvaishnav      | 2016-08-12   |

The history reflects a standard beginner GitHub workflow: initial commit → branch → edit → PR → merge.

## Branch Conventions

| Branch                                 | Purpose                              |
|----------------------------------------|--------------------------------------|
| `master`                               | Default/main branch                  |
| `claude/add-claude-documentation-dqMJp` | AI-generated documentation branch   |

Branch naming for AI-generated work follows the pattern: `claude/<short-description>-<random-suffix>`.

## Development Workflow

Since this repository has no build system, test suite, or CI pipeline, the workflow is straightforward:

1. **Create a branch** from `master` for any change.
2. **Edit files** directly (no build step needed).
3. **Commit** with a clear, descriptive message.
4. **Push** the branch to `origin`.
5. **Open a pull request** targeting `master` if review is desired.

```bash
# Example workflow
git checkout -b my-change
# ... make edits ...
git add README.md
git commit -m "Describe the change clearly"
git push -u origin my-change
```

## Git Configuration

- **Remote:** `origin` → `http://local_proxy@127.0.0.1:30256/git/kuldeepvaishnav/hello-world`
- **Default branch:** `master`
- **Author:** Kuldeep Vaishnav (`kvaishnav@gmail.com`)

## Key Conventions for AI Assistants

### What to do
- Keep changes minimal and scoped — this is a documentation-only repo.
- Follow the branch naming pattern `claude/<description>-<suffix>` for AI work.
- Always develop on the designated feature branch (not `master` directly).
- Write clear, concise commit messages in the imperative mood (e.g. "Add CLAUDE.md").
- Push to the feature branch with `git push -u origin <branch-name>`.

### What to avoid
- Do not introduce source code, build scripts, or dependencies without explicit user instruction.
- Do not push directly to `master`.
- Do not create pull requests unless the user explicitly requests one.
- Do not modify `.git/config` or other git internals.

## No Build / Test / Lint Steps

This repository has **no**:
- Build system (no `Makefile`, `package.json`, `CMakeLists.txt`, etc.)
- Test framework (no test files or test runner)
- Linter or formatter (no `.eslintrc`, `.prettierrc`, `pyproject.toml`, etc.)
- CI/CD pipeline (no `.github/workflows/`, no external CI config)

When asked to run tests, build, or lint, inform the user that none of these are configured and ask for clarification on what they'd like to set up.

## Adding Code to This Repository

If the user wants to turn this into a real project, typical next steps would be:

1. Choose a language and initialize the project (e.g., `npm init`, `poetry init`, `cargo init`).
2. Add a `.gitignore` appropriate for that language/framework.
3. Add a `README.md` with setup instructions.
4. Configure a CI workflow (e.g., GitHub Actions in `.github/workflows/`).

Offer to help with any of these steps when appropriate.
