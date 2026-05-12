FROM mcr.microsoft.com/devcontainers/base:debian

# ca-certificates, curl, git are already in the devcontainers base image.
# fd-find:  fast file finder (aliased to fd below)
# fzf:      fuzzy finder for files and command history
# gh:       GitHub CLI
# gosu:     drops privileges in the entrypoint
# jq:       JSON processor
# ripgrep:  fast recursive grep (rg)
# tmux:     terminal multiplexer
# vim:      text editor
RUN apt-get update \
    && apt-get install -y --no-install-recommends fd-find fzf gh gosu jq ripgrep tmux vim \
    && ln -s $(which fdfind) /usr/local/bin/fd \
    && rm -rf /var/lib/apt/lists/*

COPY --chmod=755 sandcat/scripts/app-init.sh /usr/local/bin/app-init.sh
COPY --chmod=755 sandcat/scripts/app-user-init.sh /usr/local/bin/app-user-init.sh
COPY --chown=vscode:vscode sandcat/tmux.conf /home/vscode/.tmux.conf

USER vscode

# Install Claude Code (native binary — no Node.js required).
RUN curl -fsSL https://claude.ai/install.sh | bash

# Install mise (SDK manager) for language toolchains.
RUN curl https://mise.run | sh
# Make mise available in login shells (su - vscode) and Docker CMD/RUN.
RUN echo 'export PATH="/home/vscode/.local/bin:/home/vscode/.local/share/mise/shims:$PATH"' >> /home/vscode/.profile
ENV PATH="/home/vscode/.local/bin:/home/vscode/.local/share/mise/shims:$PATH"

# Development stacks (managed by sandcat init --stacks):
# Versions mirror .sdkmanrc and project/build.properties on the host.
RUN mise use -g java@temurin-21.0.11
RUN mise use -g scala@2.13.18
RUN mise use -g sbt@1.12.10
RUN mise use -g node@20
RUN mise use -g just@1.36.0
RUN mise use -g flyway@12.5.0
# END STACKS

# Project-specific tooling on top of the language stacks:
# - markdownlint-cli2 is invoked by `just style-check` / `just style-fix`.
RUN npm install -g markdownlint-cli2@0.22.1

# If Java was installed above, bake JAVA_HOME and JAVA_TOOL_OPTIONS into
# .bashrc so VS Code's env probe picks them up before the entrypoint runs.
# Without JAVA_HOME, JVM tooling like Metals fails to find the JDK.
# JAVA_TOOL_OPTIONS points to a trust store copy that the entrypoint will
# populate with the mitmproxy CA at runtime; until then it holds the default
# Java CAs (harmless — equivalent to not setting it at all).
# A version-independent symlink is used so .bashrc doesn't need updating
# when the Java version changes — only the symlink target is updated.
RUN if MISE_JAVA=$(mise where java 2>/dev/null); then \
    dir="$HOME/.local/share/sandcat"; mkdir -p "$dir"; \
    ln -sfn "$MISE_JAVA" "$dir/java-home"; \
    cp "$MISE_JAVA/lib/security/cacerts" "$dir/cacerts" 2>/dev/null || true; \
    { echo ''; \
    echo '# sandcat-java-env'; \
    echo '[ -L "$HOME/.local/share/sandcat/java-home" ] && export JAVA_HOME="$HOME/.local/share/sandcat/java-home"'; \
    echo '[ -f "$HOME/.local/share/sandcat/cacerts" ] && export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=$HOME/.local/share/sandcat/cacerts -Djavax.net.ssl.trustStorePassword=changeit"'; \
    } >> "$HOME/.bashrc"; \
    fi

# Pre-create ~/.claude so Docker bind-mounts (CLAUDE.md, agents/, commands/)
# don't cause it to be created as root-owned.
RUN mkdir -p /home/vscode/.claude

RUN echo 'alias claude-yolo="claude --dangerously-skip-permissions"' >> /home/vscode/.bashrc

USER root
ENTRYPOINT ["/usr/local/bin/app-init.sh"]
