//! The application's native bindings to `fyde-sdk` (the `sdk` repo's `master`): exposes
//! the SDK to the Kotlin Multiplatform app via UniFFI-generated bindings,
//! which `sdkBridge` (`../build.gradle.kts`) generates from this library.
//!
//! [`FydeClient`] mirrors [`Client`] plus its users, sessions, server
//! state, documents and scripts operations; domain types are mirrored as
//! `Ffi*` records/enums since UniFFI has no native `Uuid` type and cannot
//! export the SDK's own [`Document`]/[`ChangelogEvent`] types directly.

use std::collections::HashMap;
use std::path::Path;
use std::sync::{Arc, PoisonError, RwLock};

use uuid::Uuid;

use fyde_sdk::{
    Attribute, AttributeDefinition, AttributeType, Category, ChangelogEvent, Client, ClientConfig,
    Document, DocumentType, Error, ErrorCode, ErrorContext as _, EventType, FacetCount,
    InstallScriptRequest, InstalledScript, LogLevel, LoginOutcome, Metadata, OpenSession,
    PasswordAnalysis, PasswordStrength, ProgressEvent, ProgressStep, Script, ScriptMetadata,
    ScriptParameter, ScriptParameterType, ScriptType, SearchFacets, SearchFilters, SearchOrder,
    SearchQuery, SetAttributeRequest, Storage, Subcategory, UploadRequest, Usage, system_language,
};

// Keeps the generated Kotlin package `uniffi.fyde_sdk` (the namespace
// otherwise defaults to this crate's name), from when these bindings lived
// in `fyde-sdk` itself.
uniffi::setup_scaffolding!("fyde_sdk");

macro_rules! define_ffi_error {
    ($($(#[$meta:meta])* $name:ident = $value:literal,)*) => {
        /// Error type surfaced to FFI callers, mirroring [`Error`]: one
        /// variant per [`ErrorCode`] (so Kotlin can catch e.g.
        /// `FfiException.InvalidCredentials` specifically), each carrying
        /// the failure's user-facing wording in the user's selected
        /// language (see [`Client::user_message`]) as its message. A flat
        /// error, so Kotlin's `Exception.message` is exactly that wording —
        /// a record-style error would instead get a generated
        /// `"field=value, ..."` message. The technical description never
        /// crosses the boundary; it's logged (and so reaches
        /// [`FfiClientConfig`]'s log listener) instead.
        #[derive(Debug, thiserror::Error, uniffi::Error)]
        #[uniffi(flat_error)]
        pub enum FfiError {
            $($(#[$meta])* #[error("{0}")] $name(String),)*
        }

        impl FfiError {
            fn new(code: ErrorCode, user_message: &str) -> Self {
                match code {
                    $(ErrorCode::$name => FfiError::$name(user_message.to_string()),)*
                    // `ErrorCode` is `#[non_exhaustive]`, but both enums are
                    // generated from the same `for_each_error_code!` list.
                    _ => unreachable!("{code:?} is missing from for_each_error_code!"),
                }
            }

            #[cfg(test)]
            fn code(&self) -> ErrorCode {
                match self {
                    $(FfiError::$name(_) => ErrorCode::$name,)*
                }
            }
        }
    };
}
fyde_sdk::for_each_error_code!(define_ffi_error);

impl FfiError {
    /// Converts `err` for an FFI caller, worded in `language`, logging its
    /// technical description first since that's otherwise lost here.
    fn from_error(err: &Error, language: &str) -> Self {
        tracing::warn!(code = %err.code(), "{err}");
        Self::new(err.code(), err.user_message(language))
    }
}

fn parse_uuid(id: &str) -> fyde_sdk::Result<Uuid> {
    Uuid::parse_str(id).with_context(|| format!("invalid id {id:?}"))
}

/// The part of life a document is about, mirroring [`Category`] across the
/// FFI boundary.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiCategory {
    Housing,
    Telecom,
    Money,
    Taxes,
    Health,
    Work,
    Family,
    Identity,
    Legal,
    Transport,
    Pets,
    Shopping,
    Leisure,
    Education,
    Subscriptions,
    Donations,
}

impl From<Category> for FfiCategory {
    fn from(category: Category) -> Self {
        match category {
            Category::Housing => Self::Housing,
            Category::Telecom => Self::Telecom,
            Category::Money => Self::Money,
            Category::Taxes => Self::Taxes,
            Category::Health => Self::Health,
            Category::Work => Self::Work,
            Category::Family => Self::Family,
            Category::Identity => Self::Identity,
            Category::Legal => Self::Legal,
            Category::Transport => Self::Transport,
            Category::Pets => Self::Pets,
            Category::Shopping => Self::Shopping,
            Category::Leisure => Self::Leisure,
            Category::Education => Self::Education,
            Category::Subscriptions => Self::Subscriptions,
            Category::Donations => Self::Donations,
        }
    }
}

impl From<FfiCategory> for Category {
    fn from(category: FfiCategory) -> Self {
        match category {
            FfiCategory::Housing => Self::Housing,
            FfiCategory::Telecom => Self::Telecom,
            FfiCategory::Money => Self::Money,
            FfiCategory::Taxes => Self::Taxes,
            FfiCategory::Health => Self::Health,
            FfiCategory::Work => Self::Work,
            FfiCategory::Family => Self::Family,
            FfiCategory::Identity => Self::Identity,
            FfiCategory::Legal => Self::Legal,
            FfiCategory::Transport => Self::Transport,
            FfiCategory::Pets => Self::Pets,
            FfiCategory::Shopping => Self::Shopping,
            FfiCategory::Leisure => Self::Leisure,
            FfiCategory::Education => Self::Education,
            FfiCategory::Subscriptions => Self::Subscriptions,
            FfiCategory::Donations => Self::Donations,
        }
    }
}

/// A finer-grained classification within a document's [`FfiCategory`],
/// mirroring [`Subcategory`] across the FFI boundary.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiSubcategory {
    Energy,
    Water,
    HomeInsurance,
    HomeServices,
    Rent,
    Property,
    Works,
    Mobile,
    Internet,
    Banking,
    Savings,
    Investments,
    Loans,
    Gaming,
    IncomeTax,
    TaxCertificates,
    PropertyTax,
    HousingTax,
    HealthInsurance,
    ComplementaryHealth,
    Care,
    DeathDisabilityCover,
    Payslips,
    Employment,
    HouseholdEmployer,
    Freelance,
    Unemployment,
    Retirement,
    Benefits,
    Childcare,
    IdDocuments,
    CivilStatus,
    InheritanceGifts,
    Company,
    Justice,
    Train,
    Flight,
    Road,
    Ride,
    Travel,
    Vehicle,
    VehicleInsurance,
    Veterinary,
    PetCare,
    PetInsurance,
    PetSupplies,
    Online,
    Groceries,
    FoodDelivery,
    Electronics,
    HomeEquipment,
    Clothing,
    Secondhand,
    Sports,
    Events,
    Associations,
    School,
    HigherEducation,
    Training,
    Media,
    CloudSoftware,
    Press,
}

impl From<Subcategory> for FfiSubcategory {
    fn from(subcategory: Subcategory) -> Self {
        match subcategory {
            Subcategory::Energy => Self::Energy,
            Subcategory::Water => Self::Water,
            Subcategory::HomeInsurance => Self::HomeInsurance,
            Subcategory::HomeServices => Self::HomeServices,
            Subcategory::Rent => Self::Rent,
            Subcategory::Property => Self::Property,
            Subcategory::Works => Self::Works,
            Subcategory::Mobile => Self::Mobile,
            Subcategory::Internet => Self::Internet,
            Subcategory::Banking => Self::Banking,
            Subcategory::Savings => Self::Savings,
            Subcategory::Investments => Self::Investments,
            Subcategory::Loans => Self::Loans,
            Subcategory::Gaming => Self::Gaming,
            Subcategory::IncomeTax => Self::IncomeTax,
            Subcategory::TaxCertificates => Self::TaxCertificates,
            Subcategory::PropertyTax => Self::PropertyTax,
            Subcategory::HousingTax => Self::HousingTax,
            Subcategory::HealthInsurance => Self::HealthInsurance,
            Subcategory::ComplementaryHealth => Self::ComplementaryHealth,
            Subcategory::Care => Self::Care,
            Subcategory::DeathDisabilityCover => Self::DeathDisabilityCover,
            Subcategory::Payslips => Self::Payslips,
            Subcategory::Employment => Self::Employment,
            Subcategory::HouseholdEmployer => Self::HouseholdEmployer,
            Subcategory::Freelance => Self::Freelance,
            Subcategory::Unemployment => Self::Unemployment,
            Subcategory::Retirement => Self::Retirement,
            Subcategory::Benefits => Self::Benefits,
            Subcategory::Childcare => Self::Childcare,
            Subcategory::IdDocuments => Self::IdDocuments,
            Subcategory::CivilStatus => Self::CivilStatus,
            Subcategory::InheritanceGifts => Self::InheritanceGifts,
            Subcategory::Company => Self::Company,
            Subcategory::Justice => Self::Justice,
            Subcategory::Train => Self::Train,
            Subcategory::Flight => Self::Flight,
            Subcategory::Road => Self::Road,
            Subcategory::Ride => Self::Ride,
            Subcategory::Travel => Self::Travel,
            Subcategory::Vehicle => Self::Vehicle,
            Subcategory::VehicleInsurance => Self::VehicleInsurance,
            Subcategory::Veterinary => Self::Veterinary,
            Subcategory::PetCare => Self::PetCare,
            Subcategory::PetInsurance => Self::PetInsurance,
            Subcategory::PetSupplies => Self::PetSupplies,
            Subcategory::Online => Self::Online,
            Subcategory::Groceries => Self::Groceries,
            Subcategory::FoodDelivery => Self::FoodDelivery,
            Subcategory::Electronics => Self::Electronics,
            Subcategory::HomeEquipment => Self::HomeEquipment,
            Subcategory::Clothing => Self::Clothing,
            Subcategory::Secondhand => Self::Secondhand,
            Subcategory::Sports => Self::Sports,
            Subcategory::Events => Self::Events,
            Subcategory::Associations => Self::Associations,
            Subcategory::School => Self::School,
            Subcategory::HigherEducation => Self::HigherEducation,
            Subcategory::Training => Self::Training,
            Subcategory::Media => Self::Media,
            Subcategory::CloudSoftware => Self::CloudSoftware,
            Subcategory::Press => Self::Press,
        }
    }
}

impl From<FfiSubcategory> for Subcategory {
    fn from(subcategory: FfiSubcategory) -> Self {
        match subcategory {
            FfiSubcategory::Energy => Self::Energy,
            FfiSubcategory::Water => Self::Water,
            FfiSubcategory::HomeInsurance => Self::HomeInsurance,
            FfiSubcategory::HomeServices => Self::HomeServices,
            FfiSubcategory::Rent => Self::Rent,
            FfiSubcategory::Property => Self::Property,
            FfiSubcategory::Works => Self::Works,
            FfiSubcategory::Mobile => Self::Mobile,
            FfiSubcategory::Internet => Self::Internet,
            FfiSubcategory::Banking => Self::Banking,
            FfiSubcategory::Savings => Self::Savings,
            FfiSubcategory::Investments => Self::Investments,
            FfiSubcategory::Loans => Self::Loans,
            FfiSubcategory::Gaming => Self::Gaming,
            FfiSubcategory::IncomeTax => Self::IncomeTax,
            FfiSubcategory::TaxCertificates => Self::TaxCertificates,
            FfiSubcategory::PropertyTax => Self::PropertyTax,
            FfiSubcategory::HousingTax => Self::HousingTax,
            FfiSubcategory::HealthInsurance => Self::HealthInsurance,
            FfiSubcategory::ComplementaryHealth => Self::ComplementaryHealth,
            FfiSubcategory::Care => Self::Care,
            FfiSubcategory::DeathDisabilityCover => Self::DeathDisabilityCover,
            FfiSubcategory::Payslips => Self::Payslips,
            FfiSubcategory::Employment => Self::Employment,
            FfiSubcategory::HouseholdEmployer => Self::HouseholdEmployer,
            FfiSubcategory::Freelance => Self::Freelance,
            FfiSubcategory::Unemployment => Self::Unemployment,
            FfiSubcategory::Retirement => Self::Retirement,
            FfiSubcategory::Benefits => Self::Benefits,
            FfiSubcategory::Childcare => Self::Childcare,
            FfiSubcategory::IdDocuments => Self::IdDocuments,
            FfiSubcategory::CivilStatus => Self::CivilStatus,
            FfiSubcategory::InheritanceGifts => Self::InheritanceGifts,
            FfiSubcategory::Company => Self::Company,
            FfiSubcategory::Justice => Self::Justice,
            FfiSubcategory::Train => Self::Train,
            FfiSubcategory::Flight => Self::Flight,
            FfiSubcategory::Road => Self::Road,
            FfiSubcategory::Ride => Self::Ride,
            FfiSubcategory::Travel => Self::Travel,
            FfiSubcategory::Vehicle => Self::Vehicle,
            FfiSubcategory::VehicleInsurance => Self::VehicleInsurance,
            FfiSubcategory::Veterinary => Self::Veterinary,
            FfiSubcategory::PetCare => Self::PetCare,
            FfiSubcategory::PetInsurance => Self::PetInsurance,
            FfiSubcategory::PetSupplies => Self::PetSupplies,
            FfiSubcategory::Online => Self::Online,
            FfiSubcategory::Groceries => Self::Groceries,
            FfiSubcategory::FoodDelivery => Self::FoodDelivery,
            FfiSubcategory::Electronics => Self::Electronics,
            FfiSubcategory::HomeEquipment => Self::HomeEquipment,
            FfiSubcategory::Clothing => Self::Clothing,
            FfiSubcategory::Secondhand => Self::Secondhand,
            FfiSubcategory::Sports => Self::Sports,
            FfiSubcategory::Events => Self::Events,
            FfiSubcategory::Associations => Self::Associations,
            FfiSubcategory::School => Self::School,
            FfiSubcategory::HigherEducation => Self::HigherEducation,
            FfiSubcategory::Training => Self::Training,
            FfiSubcategory::Media => Self::Media,
            FfiSubcategory::CloudSoftware => Self::CloudSoftware,
            FfiSubcategory::Press => Self::Press,
        }
    }
}

/// What kind of paper a document is, mirroring [`DocumentType`] across the
/// FFI boundary.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiDocumentType {
    Invoice,
    Receipt,
    Statement,
    ReimbursementStatement,
    Payslip,
    Contract,
    InsuranceCertificate,
    InsuranceCard,
    RightsCertificate,
    ProofOfResidence,
    Rib,
    TaxNotice,
    TaxReturn,
    TaxCertificate,
    EmploymentCertificate,
    FeeSummary,
    Booking,
    MedicalDocument,
    Quote,
    IdDocument,
    CivilStatusRecord,
    Letter,
    Acknowledgement,
    Form,
    PaymentSchedule,
    Mandate,
    TermsAndConditions,
    Brochure,
    Timesheet,
    InspectionReport,
    NotarialDeed,
    Attestation,
    PaymentReminder,
    Fine,
    Warranty,
    Resume,
    Minutes,
    Diploma,
    Other,
}

impl From<DocumentType> for FfiDocumentType {
    fn from(r#type: DocumentType) -> Self {
        match r#type {
            DocumentType::Invoice => Self::Invoice,
            DocumentType::Receipt => Self::Receipt,
            DocumentType::Statement => Self::Statement,
            DocumentType::ReimbursementStatement => Self::ReimbursementStatement,
            DocumentType::Payslip => Self::Payslip,
            DocumentType::Contract => Self::Contract,
            DocumentType::InsuranceCertificate => Self::InsuranceCertificate,
            DocumentType::InsuranceCard => Self::InsuranceCard,
            DocumentType::RightsCertificate => Self::RightsCertificate,
            DocumentType::ProofOfResidence => Self::ProofOfResidence,
            DocumentType::Rib => Self::Rib,
            DocumentType::TaxNotice => Self::TaxNotice,
            DocumentType::TaxReturn => Self::TaxReturn,
            DocumentType::TaxCertificate => Self::TaxCertificate,
            DocumentType::EmploymentCertificate => Self::EmploymentCertificate,
            DocumentType::FeeSummary => Self::FeeSummary,
            DocumentType::Booking => Self::Booking,
            DocumentType::MedicalDocument => Self::MedicalDocument,
            DocumentType::Quote => Self::Quote,
            DocumentType::IdDocument => Self::IdDocument,
            DocumentType::CivilStatusRecord => Self::CivilStatusRecord,
            DocumentType::Letter => Self::Letter,
            DocumentType::Acknowledgement => Self::Acknowledgement,
            DocumentType::Form => Self::Form,
            DocumentType::PaymentSchedule => Self::PaymentSchedule,
            DocumentType::Mandate => Self::Mandate,
            DocumentType::TermsAndConditions => Self::TermsAndConditions,
            DocumentType::Brochure => Self::Brochure,
            DocumentType::Timesheet => Self::Timesheet,
            DocumentType::InspectionReport => Self::InspectionReport,
            DocumentType::NotarialDeed => Self::NotarialDeed,
            DocumentType::Attestation => Self::Attestation,
            DocumentType::PaymentReminder => Self::PaymentReminder,
            DocumentType::Fine => Self::Fine,
            DocumentType::Warranty => Self::Warranty,
            DocumentType::Resume => Self::Resume,
            DocumentType::Minutes => Self::Minutes,
            DocumentType::Diploma => Self::Diploma,
            DocumentType::Other => Self::Other,
        }
    }
}

impl From<FfiDocumentType> for DocumentType {
    fn from(r#type: FfiDocumentType) -> Self {
        match r#type {
            FfiDocumentType::Invoice => Self::Invoice,
            FfiDocumentType::Receipt => Self::Receipt,
            FfiDocumentType::Statement => Self::Statement,
            FfiDocumentType::ReimbursementStatement => Self::ReimbursementStatement,
            FfiDocumentType::Payslip => Self::Payslip,
            FfiDocumentType::Contract => Self::Contract,
            FfiDocumentType::InsuranceCertificate => Self::InsuranceCertificate,
            FfiDocumentType::InsuranceCard => Self::InsuranceCard,
            FfiDocumentType::RightsCertificate => Self::RightsCertificate,
            FfiDocumentType::ProofOfResidence => Self::ProofOfResidence,
            FfiDocumentType::Rib => Self::Rib,
            FfiDocumentType::TaxNotice => Self::TaxNotice,
            FfiDocumentType::TaxReturn => Self::TaxReturn,
            FfiDocumentType::TaxCertificate => Self::TaxCertificate,
            FfiDocumentType::EmploymentCertificate => Self::EmploymentCertificate,
            FfiDocumentType::FeeSummary => Self::FeeSummary,
            FfiDocumentType::Booking => Self::Booking,
            FfiDocumentType::MedicalDocument => Self::MedicalDocument,
            FfiDocumentType::Quote => Self::Quote,
            FfiDocumentType::IdDocument => Self::IdDocument,
            FfiDocumentType::CivilStatusRecord => Self::CivilStatusRecord,
            FfiDocumentType::Letter => Self::Letter,
            FfiDocumentType::Acknowledgement => Self::Acknowledgement,
            FfiDocumentType::Form => Self::Form,
            FfiDocumentType::PaymentSchedule => Self::PaymentSchedule,
            FfiDocumentType::Mandate => Self::Mandate,
            FfiDocumentType::TermsAndConditions => Self::TermsAndConditions,
            FfiDocumentType::Brochure => Self::Brochure,
            FfiDocumentType::Timesheet => Self::Timesheet,
            FfiDocumentType::InspectionReport => Self::InspectionReport,
            FfiDocumentType::NotarialDeed => Self::NotarialDeed,
            FfiDocumentType::Attestation => Self::Attestation,
            FfiDocumentType::PaymentReminder => Self::PaymentReminder,
            FfiDocumentType::Fine => Self::Fine,
            FfiDocumentType::Warranty => Self::Warranty,
            FfiDocumentType::Resume => Self::Resume,
            FfiDocumentType::Minutes => Self::Minutes,
            FfiDocumentType::Diploma => Self::Diploma,
            FfiDocumentType::Other => Self::Other,
        }
    }
}

/// A document's cleartext metadata, mirroring [`Metadata`] across the FFI
/// boundary.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiMetadata {
    pub id: String,
    pub name: String,
    pub original_name: String,
    pub content_type: String,
    pub created_at: i64,
    pub size: u64,
    pub checksum: String,
    pub transcript: String,
    pub r#type: Option<FfiDocumentType>,
    pub category: Option<FfiCategory>,
    pub subcategory: Option<FfiSubcategory>,
    pub attributes: HashMap<String, FfiAttribute>,
}

/// One typed value of a document's attributes, mirroring [`Attribute`]
/// across the FFI boundary.
#[derive(Debug, Clone, PartialEq, uniffi::Enum)]
pub enum FfiAttribute {
    String {
        value: String,
    },
    Integer {
        value: i64,
    },
    Float {
        value: f64,
    },
    Boolean {
        value: bool,
    },
    /// An ISO 8601 `YYYY-MM-DD` calendar date.
    Date {
        value: String,
    },
    /// `minor_units` of `currency`'s ISO 4217 minor unit (cents for `"EUR"`).
    Amount {
        minor_units: i64,
        currency: String,
    },
    /// `selected`, always one of `options`.
    Select {
        selected: String,
        options: Vec<String>,
    },
}

impl From<Attribute> for FfiAttribute {
    fn from(attribute: Attribute) -> Self {
        match attribute {
            Attribute::String(value) => Self::String { value },
            Attribute::Integer(value) => Self::Integer { value },
            Attribute::Float(value) => Self::Float { value },
            Attribute::Boolean(value) => Self::Boolean { value },
            Attribute::Date(value) => Self::Date { value },
            Attribute::Amount {
                minor_units,
                currency,
            } => Self::Amount {
                minor_units,
                currency,
            },
            Attribute::Select { selected, options } => Self::Select { selected, options },
        }
    }
}

impl From<FfiAttribute> for Attribute {
    fn from(attribute: FfiAttribute) -> Self {
        match attribute {
            FfiAttribute::String { value } => Self::String(value),
            FfiAttribute::Integer { value } => Self::Integer(value),
            FfiAttribute::Float { value } => Self::Float(value),
            FfiAttribute::Boolean { value } => Self::Boolean(value),
            FfiAttribute::Date { value } => Self::Date(value),
            FfiAttribute::Amount {
                minor_units,
                currency,
            } => Self::Amount {
                minor_units,
                currency,
            },
            FfiAttribute::Select { selected, options } => Self::Select { selected, options },
        }
    }
}

/// The type an attribute's values must have, mirroring [`AttributeType`]
/// across the FFI boundary.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiAttributeType {
    String,
    Integer,
    Float,
    Boolean,
    Date,
    Amount,
    Select,
}

impl From<AttributeType> for FfiAttributeType {
    fn from(r#type: AttributeType) -> Self {
        match r#type {
            AttributeType::String => Self::String,
            AttributeType::Integer => Self::Integer,
            AttributeType::Float => Self::Float,
            AttributeType::Boolean => Self::Boolean,
            AttributeType::Date => Self::Date,
            AttributeType::Amount => Self::Amount,
            AttributeType::Select => Self::Select,
        }
    }
}

impl From<FfiAttributeType> for AttributeType {
    fn from(r#type: FfiAttributeType) -> Self {
        match r#type {
            FfiAttributeType::String => Self::String,
            FfiAttributeType::Integer => Self::Integer,
            FfiAttributeType::Float => Self::Float,
            FfiAttributeType::Boolean => Self::Boolean,
            FfiAttributeType::Date => Self::Date,
            FfiAttributeType::Amount => Self::Amount,
            FfiAttributeType::Select => Self::Select,
        }
    }
}

/// An attribute definition, mirroring [`AttributeDefinition`] across the
/// FFI boundary. `id` crosses as a string since UniFFI has no native UUID
/// type.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiAttributeDefinition {
    pub id: String,
    pub name: String,
    pub r#type: FfiAttributeType,
    pub description: String,
    pub is_default: bool,
}

impl From<AttributeDefinition> for FfiAttributeDefinition {
    fn from(definition: AttributeDefinition) -> Self {
        Self {
            id: definition.id().to_string(),
            name: definition.name().to_string(),
            r#type: definition.r#type().into(),
            description: definition.description().to_string(),
            is_default: definition.is_default(),
        }
    }
}

impl From<Metadata> for FfiMetadata {
    fn from(metadata: Metadata) -> Self {
        Self {
            id: metadata.id().to_string(),
            name: metadata.name().to_string(),
            original_name: metadata.original_name().to_string(),
            content_type: metadata.content_type().to_string(),
            created_at: metadata.created_at(),
            size: metadata.size(),
            checksum: metadata.checksum().to_string(),
            transcript: metadata.transcript().to_string(),
            r#type: metadata.r#type().map(FfiDocumentType::from),
            category: metadata.category().map(FfiCategory::from),
            subcategory: metadata.subcategory().map(FfiSubcategory::from),
            attributes: metadata
                .attributes()
                .iter()
                .map(|(name, attribute)| (name.clone(), attribute.clone().into()))
                .collect(),
        }
    }
}

impl TryFrom<FfiMetadata> for Metadata {
    type Error = Error;

    fn try_from(metadata: FfiMetadata) -> Result<Self, Self::Error> {
        Ok(Self {
            id: parse_uuid(&metadata.id)?,
            name: metadata.name,
            original_name: metadata.original_name,
            content_type: metadata.content_type,
            created_at: metadata.created_at,
            size: metadata.size,
            checksum: metadata.checksum,
            transcript: metadata.transcript,
            r#type: metadata.r#type.map(DocumentType::from),
            category: metadata.category.map(Category::from),
            subcategory: metadata.subcategory.map(Subcategory::from),
            attributes: metadata
                .attributes
                .into_iter()
                .map(|(name, attribute)| (name, attribute.into()))
                .collect(),
        })
    }
}

/// A document, mirroring [`Document`] across the FFI boundary. `id` crosses
/// as a string since UniFFI has no native UUID type.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiDocument {
    pub id: String,
    pub content: Vec<u8>,
    pub metadata: FfiMetadata,
}

impl From<Document> for FfiDocument {
    fn from(document: Document) -> Self {
        Self {
            id: document.id().to_string(),
            content: document.content().to_vec(),
            metadata: document.metadata().clone().into(),
        }
    }
}

/// Which documents a search keeps, mirroring [`SearchFilters`] across the
/// FFI boundary: any of the values within one list, every list that's
/// non-empty, and every bound that's set.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiSearchFilters {
    pub categories: Vec<FfiCategory>,
    pub subcategories: Vec<FfiSubcategory>,
    pub types: Vec<FfiDocumentType>,
    pub content_types: Vec<String>,
    pub created_from: Option<i64>,
    pub created_until: Option<i64>,
}

impl From<FfiSearchFilters> for SearchFilters {
    fn from(filters: FfiSearchFilters) -> Self {
        Self {
            categories: filters.categories.into_iter().map(Category::from).collect(),
            subcategories: filters
                .subcategories
                .into_iter()
                .map(Subcategory::from)
                .collect(),
            types: filters.types.into_iter().map(DocumentType::from).collect(),
            content_types: filters.content_types,
            created_from: filters.created_from,
            created_until: filters.created_until,
        }
    }
}

/// The order search results come back in, mirroring [`SearchOrder`].
#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FfiSearchOrder {
    Relevance,
    Newest,
    Oldest,
}

impl From<FfiSearchOrder> for SearchOrder {
    fn from(order: FfiSearchOrder) -> Self {
        match order {
            FfiSearchOrder::Relevance => Self::Relevance,
            FfiSearchOrder::Newest => Self::Newest,
            FfiSearchOrder::Oldest => Self::Oldest,
        }
    }
}

/// The signed-in account's plan and current usage, mirroring [`Usage`]
/// across the FFI boundary.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiUsage {
    pub plan_id: String,
    pub max_scripts: u32,
    pub max_sessions: u32,
    pub max_size: u64,
    pub enabled_scripts: u32,
    pub open_sessions: u32,
    pub total_size: u64,
}

impl From<Usage> for FfiUsage {
    fn from(usage: Usage) -> Self {
        Self {
            plan_id: usage.plan_id().to_string(),
            max_scripts: usage.max_scripts(),
            max_sessions: usage.max_sessions(),
            max_size: usage.max_size(),
            enabled_scripts: usage.enabled_scripts(),
            open_sessions: usage.open_sessions(),
            total_size: usage.total_size(),
        }
    }
}

/// How hard a password looks to guess, weakest first, mirroring
/// [`PasswordStrength`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiPasswordStrength {
    Weak,
    Fair,
    Strong,
    VeryStrong,
}

impl From<PasswordStrength> for FfiPasswordStrength {
    fn from(strength: PasswordStrength) -> Self {
        match strength {
            PasswordStrength::Weak => Self::Weak,
            PasswordStrength::Fair => Self::Fair,
            PasswordStrength::Strong => Self::Strong,
            PasswordStrength::VeryStrong => Self::VeryStrong,
        }
    }
}

/// What the SDK makes of a password being typed, for a strength meter,
/// mirroring [`PasswordAnalysis`]: its score (0 to 100), its strength, its
/// length in characters, and whether `create_user`/`change_password` accept
/// it.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiPasswordAnalysis {
    pub score: u8,
    pub strength: FfiPasswordStrength,
    pub length: u32,
    pub is_accepted: bool,
}

impl From<PasswordAnalysis> for FfiPasswordAnalysis {
    fn from(analysis: PasswordAnalysis) -> Self {
        Self {
            score: analysis.score(),
            strength: analysis.strength().into(),
            length: analysis.length(),
            is_accepted: analysis.is_accepted(),
        }
    }
}

/// How many documents carry one category, mirroring
/// `FacetCount<Category>` (UniFFI has no generic records).
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiCategoryCount {
    pub value: FfiCategory,
    pub count: u64,
}

/// How many documents carry one subcategory, mirroring
/// `FacetCount<Subcategory>`.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiSubcategoryCount {
    pub value: FfiSubcategory,
    pub count: u64,
}

/// How many documents carry one type, mirroring `FacetCount<DocumentType>`.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiDocumentTypeCount {
    pub value: FfiDocumentType,
    pub count: u64,
}

/// How many documents carry one free-form value (a content type),
/// mirroring `FacetCount<String>`.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiStringCount {
    pub value: String,
    pub count: u64,
}

/// The filter values still worth offering for a search, mirroring
/// [`SearchFacets`] across the FFI boundary.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiSearchFacets {
    pub categories: Vec<FfiCategoryCount>,
    pub subcategories: Vec<FfiSubcategoryCount>,
    pub types: Vec<FfiDocumentTypeCount>,
    pub content_types: Vec<FfiStringCount>,
    pub earliest_created_at: Option<i64>,
    pub latest_created_at: Option<i64>,
}

impl From<SearchFacets> for FfiSearchFacets {
    fn from(facets: SearchFacets) -> Self {
        fn strings(counts: Vec<FacetCount<String>>) -> Vec<FfiStringCount> {
            counts
                .into_iter()
                .map(|facet| FfiStringCount {
                    value: facet.value,
                    count: facet.count,
                })
                .collect()
        }

        Self {
            categories: facets
                .categories
                .into_iter()
                .map(|facet| FfiCategoryCount {
                    value: facet.value.into(),
                    count: facet.count,
                })
                .collect(),
            subcategories: facets
                .subcategories
                .into_iter()
                .map(|facet| FfiSubcategoryCount {
                    value: facet.value.into(),
                    count: facet.count,
                })
                .collect(),
            types: facets
                .types
                .into_iter()
                .map(|facet| FfiDocumentTypeCount {
                    value: facet.value.into(),
                    count: facet.count,
                })
                .collect(),
            content_types: strings(facets.content_types),
            earliest_created_at: facets.earliest_created_at,
            latest_created_at: facets.latest_created_at,
        }
    }
}

/// What kind of script this is, mirroring [`ScriptType`].
#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FfiScriptType {
    Scraper,
    Parser,
}

impl From<ScriptType> for FfiScriptType {
    fn from(script_type: ScriptType) -> Self {
        match script_type {
            ScriptType::Scraper => FfiScriptType::Scraper,
            ScriptType::Parser => FfiScriptType::Parser,
        }
    }
}

/// What kind of value a [`FfiScriptParameter`] expects, mirroring
/// [`ScriptParameterType`].
#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FfiScriptParameterType {
    String,
    Number,
    Boolean,
}

impl From<ScriptParameterType> for FfiScriptParameterType {
    fn from(parameter_type: ScriptParameterType) -> Self {
        match parameter_type {
            ScriptParameterType::String => FfiScriptParameterType::String,
            ScriptParameterType::Number => FfiScriptParameterType::Number,
            ScriptParameterType::Boolean => FfiScriptParameterType::Boolean,
        }
    }
}

/// A single configurable parameter a script declares it needs from the
/// user, mirroring [`ScriptParameter`].
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiScriptParameter {
    pub label: String,
    pub placeholder: String,
    pub parameter_type: FfiScriptParameterType,
    pub required: bool,
    pub secret: bool,
}

impl From<ScriptParameter> for FfiScriptParameter {
    fn from(parameter: ScriptParameter) -> Self {
        Self {
            label: parameter.label,
            placeholder: parameter.placeholder,
            parameter_type: parameter.parameter_type.into(),
            required: parameter.required,
            secret: parameter.secret,
        }
    }
}

/// A user-authored script, mirroring [`Script`] across the FFI boundary.
/// `id` and `icon_id` cross as strings since UniFFI has no native UUID type;
/// fetch the icon itself with [`FydeClient::fetch_icon`].
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiScript {
    pub id: String,
    pub name: String,
    pub description: String,
    pub short_description: String,
    pub is_public: bool,
    pub icon_id: Option<String>,
    pub allowed_domains: Vec<String>,
    pub version: u64,
    pub script: String,
    pub last_updated: i64,
    pub script_type: FfiScriptType,
    pub parameters: HashMap<String, FfiScriptParameter>,
}

impl From<Script> for FfiScript {
    fn from(script: Script) -> Self {
        Self {
            id: script.id().to_string(),
            name: script.name().to_string(),
            description: script.description().to_string(),
            short_description: script.short_description().to_string(),
            is_public: script.is_public(),
            icon_id: script.icon_id().map(|id| id.to_string()),
            allowed_domains: script.allowed_domains().to_vec(),
            version: script.version(),
            script: script.script().to_string(),
            last_updated: script.last_updated(),
            script_type: script.script_type().into(),
            parameters: script
                .parameters()
                .iter()
                .map(|(key, parameter)| (key.clone(), parameter.clone().into()))
                .collect(),
        }
    }
}

/// A script's metadata — everything but its content — mirroring
/// [`ScriptMetadata`] across the FFI boundary. `id` and `icon_id` cross as
/// strings since UniFFI has no native UUID type; fetch the icon itself with
/// [`FydeClient::fetch_icon`].
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiScriptMetadata {
    pub id: String,
    pub name: String,
    pub description: String,
    pub short_description: String,
    pub is_public: bool,
    pub icon_id: Option<String>,
    pub allowed_domains: Vec<String>,
    pub version: u64,
    pub last_updated: i64,
    pub script_type: FfiScriptType,
    pub parameters: HashMap<String, FfiScriptParameter>,
}

impl From<ScriptMetadata> for FfiScriptMetadata {
    fn from(metadata: ScriptMetadata) -> Self {
        Self {
            id: metadata.id().to_string(),
            name: metadata.name().to_string(),
            description: metadata.description().to_string(),
            short_description: metadata.short_description().to_string(),
            is_public: metadata.is_public(),
            icon_id: metadata.icon_id().map(|id| id.to_string()),
            allowed_domains: metadata.allowed_domains().to_vec(),
            version: metadata.version(),
            last_updated: metadata.last_updated(),
            script_type: metadata.script_type().into(),
            parameters: metadata
                .parameters()
                .iter()
                .map(|(key, parameter)| (key.clone(), parameter.clone().into()))
                .collect(),
        }
    }
}

/// A script installed for the authenticated user, mirroring
/// [`InstalledScript`] across the FFI boundary. `parameters_json` is the
/// JSON object of values the script was installed with (e.g.
/// `{"username": "alice"}`), since UniFFI has no native JSON value type.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiInstalledScript {
    pub script: FfiScript,
    pub parameters_json: String,
}

impl TryFrom<InstalledScript> for FfiInstalledScript {
    type Error = Error;

    fn try_from(installed: InstalledScript) -> Result<Self, Self::Error> {
        let parameters_json = serde_json::to_string(installed.parameters()).with_context(|| {
            format!(
                "failed to serialize parameters of installed script {}",
                installed.script().id()
            )
        })?;

        Ok(Self {
            script: installed.script().clone().into(),
            parameters_json,
        })
    }
}

/// The kind of write recorded by an [`FfiChangelogEvent`], mirroring
/// [`EventType`].
#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FfiEventType {
    SaveDocument,
    UpdateMetadata,
    Deleted,
    InstallScript,
}

impl From<EventType> for FfiEventType {
    fn from(event_type: EventType) -> Self {
        match event_type {
            EventType::SaveDocument => FfiEventType::SaveDocument,
            EventType::UpdateMetadata => FfiEventType::UpdateMetadata,
            EventType::Deleted => FfiEventType::Deleted,
            EventType::InstallScript => FfiEventType::InstallScript,
        }
    }
}

/// A single recorded write, mirroring [`ChangelogEvent`] across the FFI
/// boundary. `subject_id` is a document or script id depending on
/// `event_type` (see [`ChangelogEvent`]). `id`/`subject_id` cross as strings since
/// UniFFI has no native UUID type. `content`/`metadata` are `None` for
/// event types that don't carry them.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiChangelogEvent {
    pub id: String,
    pub event_type: FfiEventType,
    pub subject_id: String,
    pub content: Option<Vec<u8>>,
    pub metadata: Option<FfiMetadata>,
}

impl From<ChangelogEvent> for FfiChangelogEvent {
    fn from(event: ChangelogEvent) -> Self {
        Self {
            id: event.id().to_string(),
            event_type: event.event_type().into(),
            subject_id: event.subject_id().to_string(),
            content: event.content().map(<[u8]>::to_vec),
            metadata: event.metadata().cloned().map(FfiMetadata::from),
        }
    }
}

/// One of an account's open sessions, mirroring [`OpenSession`] across the
/// FFI boundary.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiOpenSession {
    /// The id to pass back as `login`'s `replace_session_id`, or to
    /// `revoke_session`.
    pub id: String,
    pub device_name: String,
    /// Unix timestamp, in seconds, of when the session was opened.
    pub created_at: i64,
    /// Whether this device is signed in with this session. Always `false`
    /// in a `SessionLimitReached` login outcome.
    pub is_current: bool,
}

impl From<OpenSession> for FfiOpenSession {
    fn from(session: OpenSession) -> Self {
        Self {
            id: session.id().to_string(),
            device_name: session.device_name().to_string(),
            created_at: session.created_at(),
            is_current: session.is_current(),
        }
    }
}

/// The outcome of [`FydeClient::login`], mirroring [`LoginOutcome`] across
/// the FFI boundary.
#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiLoginOutcome {
    /// A session was opened.
    LoggedIn { session_token: String },
    /// The account's plan allows no more open sessions, so none was opened:
    /// show `sessions` and let the user pick one to close, then call
    /// `login` again with its id as `replace_session_id`.
    SessionLimitReached {
        max_sessions: u32,
        sessions: Vec<FfiOpenSession>,
    },
}

impl From<LoginOutcome> for FfiLoginOutcome {
    fn from(outcome: LoginOutcome) -> Self {
        match outcome {
            LoginOutcome::LoggedIn { session_token } => Self::LoggedIn { session_token },
            LoginOutcome::SessionLimitReached {
                max_sessions,
                sessions,
            } => Self::SessionLimitReached {
                max_sessions,
                sessions: sessions.into_iter().map(Into::into).collect(),
            },
        }
    }
}

/// Which SQLite backend a [`FydeClient`] persists local state to, mirroring
/// [`Storage`].
#[derive(Debug, Clone, uniffi::Enum)]
pub enum FfiStorage {
    Disk { path: String },
    Memory,
}

impl From<FfiStorage> for Storage {
    fn from(storage: FfiStorage) -> Self {
        match storage {
            FfiStorage::Disk { path } => Storage::Disk(path.into()),
            FfiStorage::Memory => Storage::Memory,
        }
    }
}

/// The verbosity of the SDK's internal logs, mirroring [`LogLevel`].
#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FfiLogLevel {
    Off,
    Error,
    Warn,
    Info,
    Debug,
    Trace,
}

impl From<FfiLogLevel> for LogLevel {
    fn from(level: FfiLogLevel) -> Self {
        match level {
            FfiLogLevel::Off => LogLevel::Off,
            FfiLogLevel::Error => LogLevel::Error,
            FfiLogLevel::Warn => LogLevel::Warn,
            FfiLogLevel::Info => LogLevel::Info,
            FfiLogLevel::Debug => LogLevel::Debug,
            FfiLogLevel::Trace => LogLevel::Trace,
        }
    }
}

impl From<LogLevel> for FfiLogLevel {
    fn from(level: LogLevel) -> Self {
        match level {
            LogLevel::Off => FfiLogLevel::Off,
            LogLevel::Error => FfiLogLevel::Error,
            LogLevel::Warn => FfiLogLevel::Warn,
            LogLevel::Info => FfiLogLevel::Info,
            LogLevel::Debug => FfiLogLevel::Debug,
            LogLevel::Trace => FfiLogLevel::Trace,
        }
    }
}

/// Callback interface for [`FfiClientConfig::on_log`], implemented by
/// foreign (Kotlin/Swift) callers and invoked once per log line emitted by
/// the SDK.
#[uniffi::export(callback_interface)]
pub trait LogListener: Send + Sync {
    fn on_log(&self, level: FfiLogLevel, line: String);
}

/// Configuration for [`FydeClient::init`], mirroring [`ClientConfig`].
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiClientConfig {
    pub url: String,
    pub client_name: String,
    pub storage: FfiStorage,
    /// See [`ClientConfig::models_dir`]: never inside `storage`'s client
    /// directory, which logout empties.
    pub models_dir: String,
    pub log_level: FfiLogLevel,
}

impl From<FfiClientConfig> for ClientConfig {
    fn from(config: FfiClientConfig) -> Self {
        Self {
            url: config.url,
            client_name: config.client_name,
            storage: config.storage.into(),
            models_dir: config.models_dir.into(),
            log_level: config.log_level.into(),
            on_log: None,
            on_document_change: None,
            // Set by `FydeClient::init` from its `scraper_listener`.
            on_scraper_progress: None,
            on_scraper_question: None,
        }
    }
}

/// Callback interface for [`FydeClient::init`]'s `on_document_change`
/// parameter, implemented by foreign (Kotlin/Swift) callers and invoked once
/// per [`FfiChangelogEvent`] the SDK's automatic changelog sync consumes
/// (started internally by `create_user`/`login`, stopped by `logout`).
#[uniffi::export(callback_interface)]
pub trait DocumentChangeListener: Send + Sync {
    fn on_event(&self, event: FfiChangelogEvent);
}

/// A high-level stage of a scraper run, mirroring [`ProgressStep`] across
/// the FFI boundary.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiProgressStep {
    LoggingIn,
    CheckingSession,
    WaitingForVerificationCode,
    WaitingForCaptcha,
    ListingDocuments,
    DownloadingDocuments,
    Done,
}

impl From<ProgressStep> for FfiProgressStep {
    fn from(step: ProgressStep) -> Self {
        match step {
            ProgressStep::LoggingIn => Self::LoggingIn,
            ProgressStep::CheckingSession => Self::CheckingSession,
            ProgressStep::WaitingForVerificationCode => Self::WaitingForVerificationCode,
            ProgressStep::WaitingForCaptcha => Self::WaitingForCaptcha,
            ProgressStep::ListingDocuments => Self::ListingDocuments,
            ProgressStep::DownloadingDocuments => Self::DownloadingDocuments,
            ProgressStep::Done => Self::Done,
        }
    }
}

/// A scraper run's progress, mirroring [`ProgressEvent`]: a step starting
/// — with `message`, its wording in the user's language (see
/// [`ProgressStep::user_message`]) — or how far the current step has got.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum FfiProgressEvent {
    Step {
        step: FfiProgressStep,
        message: String,
    },
    Update {
        current: i64,
        total: i64,
    },
}

impl FfiProgressEvent {
    /// Converts `event`, wording its step (if any) in `language`.
    fn localized(event: ProgressEvent, language: &str) -> Self {
        match event {
            ProgressEvent::Step { step } => Self::Step {
                step: step.into(),
                message: step.user_message(language).to_string(),
            },
            ProgressEvent::Update { current, total } => Self::Update { current, total },
        }
    }
}

/// Callback interface for [`FydeClient::init`]'s `scraper_listener`,
/// implemented by foreign (Kotlin/Swift) callers so a scraper run started by
/// [`FydeClient::run_installed_script`] can show its progress and ask the
/// user questions (a one-time code, ...) on screen. Both are invoked from
/// the run's own blocking thread, never the caller's: `on_question` may
/// block until the user answers, and returns their answer to the script.
/// Several scripts can run at once, so both first get the id of the script
/// whose run it is; an answer only ever goes back to that run.
#[uniffi::export(callback_interface)]
pub trait ScraperListener: Send + Sync {
    fn on_progress(&self, script_id: String, event: FfiProgressEvent);
    fn on_question(&self, script_id: String, question: String) -> String;
}

/// A connection to a fyde server, exposed to Kotlin as `FydeClient`.
#[derive(uniffi::Object)]
pub struct FydeClient {
    inner: Client,
    /// The language scraper progress steps are worded in for
    /// `init`'s `scraper_listener`, refreshed from [`Client::language`]
    /// at the start of each [`Self::run_installed_script`]. The progress
    /// callback runs synchronously on the run's blocking thread, so it
    /// can't await the user's preference itself.
    scraper_language: Arc<RwLock<String>>,
}

impl FydeClient {
    /// Awaits `operation`, converting its error (if any) to an [`FfiError`]
    /// worded in the user's selected language. Every exported method's body
    /// runs through this, so none can surface an error untranslated.
    async fn localized<T>(
        &self,
        operation: impl Future<Output = fyde_sdk::Result<T>>,
    ) -> Result<T, FfiError> {
        match operation.await {
            Ok(value) => Ok(value),
            Err(err) => {
                let message = self.inner.user_message(&err).await;
                tracing::warn!(code = %err.code(), "{err}");
                Err(FfiError::new(err.code(), message))
            }
        }
    }
}

#[uniffi::export(async_runtime = "tokio")]
impl FydeClient {
    /// Connects to a fyde server per `config`. Mirrors [`Client::init`].
    /// Fails worded in the device's system language, since the user's own
    /// preference lives in the local database this is what opens.
    ///
    /// Needs `async_runtime = "tokio"`: `Client::init` goes through sqlx's
    /// connection pool, which requires an active Tokio context to spawn its
    /// background tasks on — not just a generic future poller.
    #[uniffi::constructor(async_runtime = "tokio")]
    pub async fn init(
        config: FfiClientConfig,
        on_log: Option<Box<dyn LogListener>>,
        on_document_change: Option<Box<dyn DocumentChangeListener>>,
        scraper_listener: Option<Box<dyn ScraperListener>>,
    ) -> Result<Arc<Self>, FfiError> {
        let mut config: ClientConfig = config.into();
        config.on_log = on_log.map(|listener| -> Arc<dyn Fn(LogLevel, String) + Send + Sync> {
            let listener: Arc<dyn LogListener> = Arc::from(listener);
            Arc::new(move |level, line| listener.on_log(level.into(), line))
        });
        config.on_document_change =
            on_document_change.map(|listener| -> Arc<dyn Fn(ChangelogEvent) + Send + Sync> {
                let listener: Arc<dyn DocumentChangeListener> = Arc::from(listener);
                Arc::new(move |event| listener.on_event(event.into()))
            });
        let scraper_language = Arc::new(RwLock::new(system_language()));
        if let Some(listener) = scraper_listener {
            let listener: Arc<dyn ScraperListener> = Arc::from(listener);
            let for_progress = listener.clone();
            let language = scraper_language.clone();
            config.on_scraper_progress =
                Some(Arc::new(move |script_id: &str, event: ProgressEvent| {
                    let language = language.read().unwrap_or_else(PoisonError::into_inner);
                    for_progress.on_progress(
                        script_id.to_string(),
                        FfiProgressEvent::localized(event, &language),
                    )
                }));
            config.on_scraper_question =
                Some(Arc::new(move |script_id: &str, question: String| {
                    listener.on_question(script_id.to_string(), question)
                }));
        }

        match Client::init(config).await {
            Ok(inner) => Ok(Arc::new(Self {
                inner,
                scraper_language,
            })),
            Err(err) => Err(FfiError::from_error(&err, &system_language())),
        }
    }

    /// Creates a new account and opens a session for the device named
    /// `device_name`, returning its session token. `allow_insecure_storage`
    /// lets the account's secrets fall back to the local database on a
    /// device without secure storage (see `is_secure_storage_available`).
    /// Mirrors [`fyde_sdk::UsersService::create`].
    pub async fn create_user(
        &self,
        username: String,
        password: String,
        device_name: String,
        allow_insecure_storage: bool,
    ) -> Result<String, FfiError> {
        self.localized(self.inner.users().create(
            &username,
            &password,
            &device_name,
            allow_insecure_storage,
        ))
        .await
    }

    /// Returns whether this device has a usable OS credential store for the
    /// account's secrets; when it doesn't, `create_user` needs
    /// `allow_insecure_storage`. Mirrors
    /// [`fyde_sdk::UsersService::is_secure_storage_available`].
    pub async fn is_secure_storage_available(&self) -> bool {
        self.inner.users().is_secure_storage_available().await
    }

    /// Verifies `username`/`password` and opens a session for the device
    /// named `device_name`, returning its session token — or, if the
    /// account's plan allows no more open sessions, the sessions to pick
    /// one from to close, passed back here as `replace_session_id`.
    /// `allow_insecure_storage` lets the session's secrets fall back to the
    /// local database on a device without secure storage, as for
    /// `create_user`. Mirrors [`fyde_sdk::UsersService::login`].
    pub async fn login(
        &self,
        username: String,
        password: String,
        device_name: String,
        allow_insecure_storage: bool,
        replace_session_id: Option<String>,
    ) -> Result<FfiLoginOutcome, FfiError> {
        self.localized(self.inner.users().login(
            &username,
            &password,
            &device_name,
            allow_insecure_storage,
            replace_session_id.as_deref(),
        ))
        .await
        .map(Into::into)
    }

    /// Closes the session opened by the most recent `create_user`/`login`
    /// call. Mirrors [`fyde_sdk::UsersService::logout`].
    pub async fn logout(&self) -> Result<(), FfiError> {
        self.localized(self.inner.users().logout()).await
    }

    /// Lists every session open on the signed-in account, oldest first,
    /// flagging the one this device uses. Mirrors
    /// [`fyde_sdk::UsersService::list_sessions`].
    pub async fn list_sessions(&self) -> Result<Vec<FfiOpenSession>, FfiError> {
        self.localized(async {
            let sessions = self.inner.users().list_sessions().await?;
            Ok(sessions.into_iter().map(Into::into).collect())
        })
        .await
    }

    /// Closes one of the account's sessions, logging its device out.
    /// Mirrors [`fyde_sdk::UsersService::revoke_session`].
    pub async fn revoke_session(&self, session_id: String) -> Result<(), FfiError> {
        self.localized(self.inner.users().revoke_session(&session_id))
            .await
    }

    /// Generates a new recovery key for the signed-in account, replacing any
    /// previous one, and returns it once for the user to write down: 24
    /// English BIP39 words separated by single spaces, stored nowhere.
    /// Mirrors [`fyde_sdk::UsersService::generate_recovery_key`].
    pub async fn generate_recovery_key(&self) -> Result<String, FfiError> {
        self.localized(self.inner.users().generate_recovery_key())
            .await
    }

    /// When the signed-in account's current recovery key was generated, as a
    /// Unix timestamp in seconds, or `None` if it has none. Asks the server
    /// each time (never cached). Mirrors
    /// [`fyde_sdk::UsersService::recovery_key_status`].
    pub async fn recovery_key_status(&self) -> Result<Option<i64>, FfiError> {
        self.localized(self.inner.users().recovery_key_status())
            .await
    }

    /// Changes the signed-in account's password from `old_password` to
    /// `new_password`, keeping every document readable and every other
    /// device signed in. Refuses a new password `analyze_password` doesn't
    /// accept; fails with `UsernameUnknown` for a session opened before the
    /// SDK cached its username (signing in again fixes it). Mirrors
    /// [`fyde_sdk::UsersService::change_password`].
    pub async fn change_password(
        &self,
        old_password: String,
        new_password: String,
    ) -> Result<(), FfiError> {
        self.localized(
            self.inner
                .users()
                .change_password(&old_password, &new_password),
        )
        .await
    }

    /// Rates `password` for a strength meter, the same verdict
    /// `create_user`/`change_password` enforce. Local, never fails. Mirrors
    /// [`fyde_sdk::UsersService::analyze_password`].
    pub fn analyze_password(&self, password: String) -> FfiPasswordAnalysis {
        self.inner.users().analyze_password(&password).into()
    }

    /// Persists `token` as the session token attached to every subsequent
    /// authenticated request, overwriting any token already stored.
    /// `create_user`/`login` already do this as part of opening a session;
    /// this is for restoring a previously-issued token (e.g. one a caller
    /// kept in secure storage across app restarts) without a network round
    /// trip. Mirrors [`Client::save_new_session`].
    pub async fn save_new_session(&self, token: String) -> Result<(), FfiError> {
        self.localized(self.inner.save_new_session(&token)).await
    }

    /// Returns whether a session token is currently persisted, i.e. a
    /// `create_user`/`login`/`save_new_session` call succeeded and `logout`
    /// hasn't been called since. Mirrors [`Client::is_connected`].
    pub async fn is_connected(&self) -> Result<bool, FfiError> {
        self.localized(self.inner.is_connected()).await
    }

    /// Fetches the signed-in account's plan, limits and current usage.
    /// Mirrors [`fyde_sdk::UsageService::get_usage`].
    pub async fn get_usage(&self) -> Result<FfiUsage, FfiError> {
        self.localized(async { Ok(FfiUsage::from(self.inner.usage().get_usage().await?)) })
            .await
    }

    /// Returns whether the fyde server currently responds as reachable.
    /// Mirrors [`fyde_sdk::ServerStateService::is_server_reachable`].
    pub async fn is_server_reachable(&self) -> Result<bool, FfiError> {
        self.localized(self.inner.server_state().is_server_reachable())
            .await
    }

    /// Reads the local file at `path`, encrypts it and its metadata, then
    /// publishes it as a "created" changelog event, returning its generated
    /// id. Mirrors [`fyde_sdk::DocumentsService::upload`].
    pub async fn upload_document(&self, path: String) -> Result<String, FfiError> {
        self.localized(async {
            let id = self
                .inner
                .documents()
                .upload(UploadRequest::from_path(Path::new(&path)))
                .await?;
            Ok(id.to_string())
        })
        .await
    }

    /// Like [`Self::upload_document`], also setting `attributes` on the
    /// document. Mirrors
    /// [`fyde_sdk::DocumentsService::upload`] with
    /// [`UploadRequest::attributes`].
    pub async fn upload_document_with_attributes(
        &self,
        path: String,
        attributes: HashMap<String, FfiAttribute>,
    ) -> Result<String, FfiError> {
        self.localized(async {
            let mut request = UploadRequest::from_path(Path::new(&path));
            request.attributes = attributes
                .into_iter()
                .map(|(name, attribute)| (name, attribute.into()))
                .collect();
            let id = self.inner.documents().upload(request).await?;
            Ok(id.to_string())
        })
        .await
    }

    /// Lists every attribute definition the signed-in user can use: the
    /// default ones, then its own. Mirrors [`fyde_sdk::AttributesService::list`].
    pub async fn list_attributes(&self) -> Result<Vec<FfiAttributeDefinition>, FfiError> {
        self.localized(async {
            let definitions = self.inner.attributes().list().await?;
            Ok(definitions.into_iter().map(Into::into).collect())
        })
        .await
    }

    /// Defines (or redefines) an attribute for the signed-in user. Mirrors
    /// [`fyde_sdk::AttributesService::set`].
    pub async fn set_attribute(
        &self,
        name: String,
        r#type: FfiAttributeType,
        description: String,
    ) -> Result<FfiAttributeDefinition, FfiError> {
        self.localized(async {
            let definition = self
                .inner
                .attributes()
                .set(SetAttributeRequest {
                    name,
                    r#type: r#type.into(),
                    description,
                })
                .await?;
            Ok(definition.into())
        })
        .await
    }

    /// Fetches a document previously cached locally by the SDK's automatic
    /// changelog sync (see [`FydeClient::init`]'s `on_document_change`), or
    /// `None` if it doesn't exist. Mirrors [`fyde_sdk::DocumentsService::get`].
    pub async fn get_document(&self, id: String) -> Result<Option<FfiDocument>, FfiError> {
        self.localized(async {
            let id = parse_uuid(&id)?;
            let document = self.inner.documents().get(id).await?;
            Ok(document.map(FfiDocument::from))
        })
        .await
    }

    /// Lists the metadata of documents previously cached locally, oldest
    /// first, one page at a time, without their content — fetch that with
    /// [`Self::get_document`] once a document is shown. Mirrors
    /// [`fyde_sdk::DocumentsService::list`].
    pub async fn list_documents(
        &self,
        offset: i64,
        limit: i64,
    ) -> Result<Vec<FfiMetadata>, FfiError> {
        self.localized(async {
            let documents = self.inner.documents().list(offset, limit).await?;
            let _span = tracing::debug_span!("to_ffi", documents = documents.len()).entered();
            Ok(documents.into_iter().map(FfiMetadata::from).collect())
        })
        .await
    }

    /// Searches documents previously cached locally for `text`, keeping
    /// only those matching `filters`, one page at a time in `order`,
    /// returning their metadata without their content (see
    /// [`Self::list_documents`]). Mirrors [`fyde_sdk::DocumentsService::search`].
    pub async fn search_documents(
        &self,
        text: String,
        filters: FfiSearchFilters,
        order: FfiSearchOrder,
        offset: i64,
        limit: i64,
    ) -> Result<Vec<FfiMetadata>, FfiError> {
        self.localized(async {
            let documents = self
                .inner
                .documents()
                .search(SearchQuery {
                    text,
                    filters: filters.into(),
                    order: order.into(),
                    offset,
                    limit,
                })
                .await?;
            Ok(documents.into_iter().map(FfiMetadata::from).collect())
        })
        .await
    }

    /// Lists the filter values still worth offering for a search of `text`
    /// narrowed by `filters` — each field's values ignoring that field's own
    /// filter. Mirrors [`fyde_sdk::DocumentsService::search_facets`].
    pub async fn search_document_facets(
        &self,
        text: String,
        filters: FfiSearchFilters,
    ) -> Result<FfiSearchFacets, FfiError> {
        self.localized(async {
            let facets = self
                .inner
                .documents()
                .search_facets(&text, &filters.into())
                .await?;
            Ok(facets.into())
        })
        .await
    }

    /// Encrypts `metadata` as given and publishes it as an "update metadata"
    /// changelog event, replacing whatever metadata was previously
    /// associated with `metadata.id`. Mirrors
    /// [`fyde_sdk::DocumentsService::update_metadata`].
    pub async fn update_document_metadata(&self, metadata: FfiMetadata) -> Result<(), FfiError> {
        self.localized(async {
            let metadata = Metadata::try_from(metadata)?;
            self.inner.documents().update_metadata(metadata).await
        })
        .await
    }

    /// Publishes the deletion of the document `id` as a changelog event: it
    /// leaves the local cache once the SDK's automatic sync consumes it,
    /// which `on_document_change` then reports as an
    /// [`FfiEventType::Deleted`] event. Mirrors
    /// [`fyde_sdk::DocumentsService::delete`].
    pub async fn delete_document(&self, id: String) -> Result<(), FfiError> {
        self.localized(async {
            let id = parse_uuid(&id)?;
            self.inner.documents().delete(id).await
        })
        .await
    }

    /// Persists `language` as this device's UI language preference — also
    /// the language every later [`FfiError`] is worded in. Mirrors
    /// [`fyde_sdk::UsersService::set_language`].
    pub async fn set_language(&self, language: String) -> Result<(), FfiError> {
        self.localized(self.inner.users().set_language(&language))
            .await
    }

    /// Returns this device's UI language preference, or `None` if never
    /// set. Mirrors [`fyde_sdk::UsersService::get_language`].
    pub async fn get_language(&self) -> Result<Option<String>, FfiError> {
        self.localized(self.inner.users().get_language()).await
    }

    /// Fetches the script matching `id`. Mirrors
    /// [`fyde_sdk::ScriptsService::fetch_script`].
    pub async fn fetch_script(&self, id: String) -> Result<FfiScript, FfiError> {
        self.localized(async {
            let id = parse_uuid(&id)?;
            let script = self.inner.scripts().fetch_script(id).await?;
            Ok(script.into())
        })
        .await
    }

    /// Fetches the icon matching `id` (an encoded image, e.g. a script's
    /// `icon_id`), from the local cache if it's there. Mirrors
    /// [`fyde_sdk::IconsService::fetch_icon`].
    pub async fn fetch_icon(&self, id: String) -> Result<Vec<u8>, FfiError> {
        self.localized(async {
            let id = parse_uuid(&id)?;
            let icon = self.inner.icons().fetch_icon(id).await?;
            Ok(icon.content().to_vec())
        })
        .await
    }

    /// Enables `script_id` for the authenticated user. Mirrors
    /// [`fyde_sdk::ScriptsService::enable_script`].
    pub async fn enable_script(&self, script_id: String) -> Result<(), FfiError> {
        self.localized(async {
            let script_id = parse_uuid(&script_id)?;
            self.inner.scripts().enable_script(script_id).await?;
            Ok(())
        })
        .await
    }

    /// Disables `script_id` for the authenticated user. Mirrors
    /// [`fyde_sdk::ScriptsService::disable_script`].
    pub async fn disable_script(&self, script_id: String) -> Result<(), FfiError> {
        self.localized(async {
            let script_id = parse_uuid(&script_id)?;
            self.inner.scripts().disable_script(script_id).await?;
            Ok(())
        })
        .await
    }

    /// Lists the metadata of the scripts currently enabled for the
    /// authenticated user, without their content. Mirrors
    /// [`fyde_sdk::ScriptsService::list_user_scripts`].
    pub async fn list_user_scripts(&self) -> Result<Vec<FfiScriptMetadata>, FfiError> {
        self.localized(async {
            let scripts = self.inner.scripts().list_user_scripts().await?;
            Ok(scripts.into_iter().map(FfiScriptMetadata::from).collect())
        })
        .await
    }

    /// Lists the metadata of every script marked public, regardless of who
    /// owns it or whether the authenticated user has it enabled, without
    /// their content. Mirrors [`fyde_sdk::ScriptsService::list_public_scripts`].
    pub async fn list_public_scripts(&self) -> Result<Vec<FfiScriptMetadata>, FfiError> {
        self.localized(async {
            let scripts = self.inner.scripts().list_public_scripts().await?;
            Ok(scripts.into_iter().map(FfiScriptMetadata::from).collect())
        })
        .await
    }

    /// Installs `script_id` for the authenticated user, to be run with
    /// `parameters_json` — a JSON object keyed by the script's parameter
    /// names (e.g. `{"username": "alice"}`). Mirrors
    /// [`fyde_sdk::ScriptsService::install_script`]: the installation only
    /// shows up in [`Self::list_installed_scripts`] once the background
    /// sync job has consumed it back.
    pub async fn install_script(
        &self,
        script_id: String,
        parameters_json: String,
    ) -> Result<(), FfiError> {
        self.localized(async {
            let script_id = parse_uuid(&script_id)?;
            let parameters = serde_json::from_str(&parameters_json).with_context(|| {
                format!("invalid parameters json for script {script_id}: expected a JSON object")
            })?;

            self.inner
                .scripts()
                .install_script(InstallScriptRequest {
                    script_id,
                    parameters,
                })
                .await?;
            Ok(())
        })
        .await
    }

    /// Lists every script installed for the authenticated user that's been
    /// synced to this device so far. Mirrors
    /// [`fyde_sdk::ScriptsService::list_installed_scripts`].
    pub async fn list_installed_scripts(&self) -> Result<Vec<FfiInstalledScript>, FfiError> {
        self.localized(async {
            let installed = self.inner.scripts().list_installed_scripts().await?;
            installed
                .into_iter()
                .map(FfiInstalledScript::try_from)
                .collect()
        })
        .await
    }

    /// Runs the installed script `script_id` — with the parameter values it
    /// was installed with — until it finishes, reporting progress and
    /// questions through `init`'s `scraper_listener`; the documents it
    /// saves are uploaded like any other. Fails if the installation hasn't
    /// synced to this device yet. Mirrors
    /// [`fyde_sdk::ScriptsService::fetch_installed_script`] then
    /// [`fyde_sdk::ScrapersService::run_installed`].
    pub async fn run_installed_script(&self, script_id: String) -> Result<(), FfiError> {
        self.localized(async {
            let script_id = parse_uuid(&script_id)?;
            let installed = self
                .inner
                .scripts()
                .fetch_installed_script(script_id)
                .await?;
            *self
                .scraper_language
                .write()
                .unwrap_or_else(PoisonError::into_inner) = self.inner.language().await;
            self.inner.scrapers().run_installed(&installed).await
        })
        .await
    }

    /// Stops the run of the installed script `script_id` that
    /// [`Self::run_installed_script`] started (and one of it waiting for its
    /// turn), returning whether there was one. Returns at once: that
    /// `run_installed_script` call then fails with `ScraperRunStopped` once
    /// its script has really stopped (unless it finished first). A question
    /// it was waiting on is abandoned, its answer dropped: the caller should
    /// dismiss it. Mirrors [`fyde_sdk::ScrapersService::stop`].
    pub async fn stop_script_run(&self, script_id: String) -> Result<bool, FfiError> {
        self.localized(async {
            // Canonical form, as `run_installed` names the run after the
            // script's id.
            let script_id = parse_uuid(&script_id)?;
            Ok(self.inner.scrapers().stop(&script_id.to_string()))
        })
        .await
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use fyde_sdk::testing::{
        FakeChangelogEvent, FakeDocument, FakeMetadata, FakeOpenSession, FakeScript,
    };

    #[test]
    fn client_config_conversion_preserves_every_field() {
        let ffi = FfiClientConfig {
            url: "https://fyde.example".into(),
            client_name: "android".into(),
            storage: FfiStorage::Disk {
                path: "/data/fyde".into(),
            },
            models_dir: "/cache/fyde/models".into(),
            log_level: FfiLogLevel::Debug,
        };

        let config = ClientConfig::from(ffi);

        assert_eq!(config.url, "https://fyde.example");
        assert_eq!(config.client_name, "android");
        assert_eq!(config.storage, Storage::Disk("/data/fyde".into()));
        assert_eq!(config.models_dir, std::path::Path::new("/cache/fyde/models"));
        assert_eq!(config.log_level, LogLevel::Debug);
    }

    #[test]
    fn parse_uuid_accepts_a_canonical_uuid() {
        let id = Uuid::now_v7();

        assert_eq!(parse_uuid(&id.to_string()).unwrap(), id);
    }

    #[test]
    fn parse_uuid_rejects_malformed_input() {
        assert!(parse_uuid("not-a-uuid").is_err());
    }

    #[test]
    fn metadata_conversion_preserves_every_field() {
        let metadata = FakeMetadata::new().build();

        let ffi: FfiMetadata = metadata.clone().into();

        assert_eq!(ffi.id, metadata.id().to_string());
        assert_eq!(ffi.name, metadata.name());
        assert_eq!(ffi.content_type, metadata.content_type());
        assert_eq!(ffi.created_at, metadata.created_at());
        assert_eq!(ffi.size, metadata.size());
        assert_eq!(ffi.checksum, metadata.checksum());
        assert_eq!(ffi.transcript, metadata.transcript());
        assert_eq!(ffi.r#type, metadata.r#type().map(FfiDocumentType::from));
        assert_eq!(ffi.category, metadata.category().map(FfiCategory::from));
        assert_eq!(
            ffi.subcategory,
            metadata.subcategory().map(FfiSubcategory::from)
        );
    }

    #[test]
    fn metadata_conversion_roundtrips_through_ffi() {
        let metadata = FakeMetadata::new().build();

        let ffi: FfiMetadata = metadata.clone().into();
        let roundtripped = Metadata::try_from(ffi).unwrap();

        assert_eq!(roundtripped, metadata);
    }

    #[test]
    fn metadata_conversion_rejects_a_malformed_id() {
        let mut ffi: FfiMetadata = FakeMetadata::new().build().into();
        ffi.id = "not-a-uuid".to_string();

        assert!(Metadata::try_from(ffi).is_err());
    }

    #[test]
    fn document_conversion_stringifies_the_id() {
        let document = FakeDocument::new()
            .with_content(b"hello".to_vec())
            .with_metadata(FakeMetadata::new().build())
            .build();

        let ffi: FfiDocument = document.clone().into();

        assert_eq!(ffi.id, document.id().to_string());
        assert_eq!(ffi.content, document.content());
        assert_eq!(ffi.metadata.name, document.metadata().name());
    }

    #[test]
    fn script_conversion_preserves_every_field() {
        let script = FakeScript::new().build();

        let ffi: FfiScript = script.clone().into();

        assert_eq!(ffi.id, script.id().to_string());
        assert_eq!(ffi.name, script.name());
        assert_eq!(ffi.description, script.description());
        assert_eq!(ffi.short_description, script.short_description());
        assert_eq!(ffi.is_public, script.is_public());
        assert_eq!(ffi.icon_id, script.icon_id().map(|id| id.to_string()));
        assert_eq!(ffi.allowed_domains, script.allowed_domains());
        assert_eq!(ffi.version, script.version());
        assert_eq!(ffi.script, script.script());
        assert_eq!(ffi.last_updated, script.last_updated());
    }

    #[test]
    fn login_outcome_conversion_preserves_the_open_sessions() {
        let session = FakeOpenSession::new().build();

        let ffi = FfiLoginOutcome::from(LoginOutcome::SessionLimitReached {
            max_sessions: 3,
            sessions: vec![session.clone()],
        });

        let FfiLoginOutcome::SessionLimitReached {
            max_sessions,
            sessions,
        } = ffi
        else {
            panic!("expected SessionLimitReached, got {ffi:?}");
        };
        assert_eq!(max_sessions, 3);
        assert_eq!(sessions.len(), 1);
        assert_eq!(sessions[0].id, session.id());
        assert_eq!(sessions[0].device_name, session.device_name());
        assert_eq!(sessions[0].created_at, session.created_at());
    }

    #[test]
    fn login_outcome_conversion_keeps_the_session_token() {
        let ffi = FfiLoginOutcome::from(LoginOutcome::LoggedIn {
            session_token: "a-token".to_string(),
        });

        assert!(matches!(
            ffi,
            FfiLoginOutcome::LoggedIn { session_token } if session_token == "a-token"
        ));
    }

    #[test]
    fn event_type_conversion_maps_every_variant() {
        assert!(matches!(
            FfiEventType::from(EventType::SaveDocument),
            FfiEventType::SaveDocument
        ));
        assert!(matches!(
            FfiEventType::from(EventType::UpdateMetadata),
            FfiEventType::UpdateMetadata
        ));
        assert!(matches!(
            FfiEventType::from(EventType::Deleted),
            FfiEventType::Deleted
        ));
    }

    #[test]
    fn changelog_event_conversion_stringifies_the_subject_id_and_preserves_payload() {
        let id = Uuid::now_v7();
        let document_id = Uuid::now_v7();
        let event = FakeChangelogEvent::new()
            .with_id(id)
            .with_event_type(EventType::SaveDocument)
            .with_subject_id(document_id)
            .with_content(b"body".to_vec())
            .with_metadata(FakeMetadata::new().build())
            .build();

        let ffi: FfiChangelogEvent = event.into();

        assert_eq!(ffi.id, id.to_string());
        assert!(matches!(ffi.event_type, FfiEventType::SaveDocument));
        assert_eq!(ffi.subject_id, document_id.to_string());
        assert_eq!(ffi.content, Some(b"body".to_vec()));
        assert!(ffi.metadata.is_some());
    }

    #[test]
    fn changelog_event_conversion_handles_events_with_no_content_or_metadata() {
        let event = FakeChangelogEvent::new()
            .with_event_type(EventType::Deleted)
            .with_subject_id(Uuid::now_v7())
            .without_content()
            .without_metadata()
            .build();

        let ffi: FfiChangelogEvent = event.into();

        assert_eq!(ffi.content, None);
        assert!(ffi.metadata.is_none());
    }

    #[test]
    fn ffi_error_carries_the_code_and_the_localized_wording() {
        let err = parse_uuid("not-a-uuid").unwrap_err();

        let ffi_err = FfiError::from_error(&err, "fr");

        assert_eq!(ffi_err.code(), ErrorCode::InvalidUuid);
        assert_eq!(ffi_err.to_string(), err.user_message("fr"));
    }

    #[test]
    fn ffi_error_never_carries_the_technical_description() {
        let err = parse_uuid("not-a-uuid").unwrap_err();
        assert!(err.to_string().contains("not-a-uuid"));

        let ffi_err = FfiError::from_error(&err, "en");

        assert!(!ffi_err.to_string().contains("not-a-uuid"));
    }

    #[test]
    fn ffi_error_has_a_variant_for_every_error_code() {
        for &code in ErrorCode::ALL {
            assert_eq!(FfiError::new(code, "wording").code(), code);
        }
    }

    #[test]
    fn password_strength_conversion_maps_every_strength() {
        for (strength, expected) in [
            (PasswordStrength::Weak, FfiPasswordStrength::Weak),
            (PasswordStrength::Fair, FfiPasswordStrength::Fair),
            (PasswordStrength::Strong, FfiPasswordStrength::Strong),
            (
                PasswordStrength::VeryStrong,
                FfiPasswordStrength::VeryStrong,
            ),
        ] {
            assert_eq!(FfiPasswordStrength::from(strength), expected);
        }
    }

    #[test]
    fn parse_uuid_failure_maps_to_invalid_uuid() {
        let err = parse_uuid("not-a-uuid").unwrap_err();

        assert_eq!(err.code(), ErrorCode::InvalidUuid);
    }
}
