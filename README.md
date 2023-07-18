<p>
  <a href="https://piiano.com/pii-data-privacy-vault/">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://docs.piiano.com/img/logo-developers-dark.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://docs.piiano.com/img/logo-developers.svg">
      <img alt="Piiano Vault" src="https://docs.piiano.com/img/logo-developers.svg" height="40" />
    </picture>
  </a>
</p>

# vault-code-samples-getting-started
## Background
Piiano Vault is the secure home for sensitive personal data. It allows you to safely store sensitive personal data in your own cloud environment with
automated compliance controls.
Vault is deployed within your own architecture, next to other DBs used by the applications, and should be used to store the most critical sensitive personal data, such as credit cards and bank account numbers, names, emails, national IDs (e.g. SSN), phone numbers, etc.
The main benefits are:
- Field level encryption, including key rotation
- Searchability is allowed over the encrypted data
- Full audit log for all data accesses
- Granular access controls
- Easy masking and tokenization of data
- Out of the box privacy compliance functionality

More details can be found [on our website](https://piiano.com/pii-data-privacy-vault/) and on the [developers portal](https://piiano.com/docs/).

## Overview

This repository contains code samples for connecting to Piiano Vault using SDKs that are auto generated from the Vault's openapi.yaml file. 
The repository is organized with a folder per language. In each language you will find several samples, including an implementation of the https://piiano.com/docs/guides/get-started flow that is based there on the Piiano Vault CLI.

For Java, we have included additional test examples of how to work with tokens. 

## Usage

Run Piiano Vault. The simplest way is using the pvault-dev flavor as explained here: https://piiano.com/docs/guides/get-started#install-piiano-vault

Assuming the Vault is running on localhost listening on port 8123 (the default):

Enter the directory of the desired code sample language.
```
cd python / java / typescript
```

And run the following commands:
```
./prepare.sh
./run.sh
```
