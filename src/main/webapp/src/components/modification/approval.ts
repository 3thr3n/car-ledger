import { TFunction } from 'i18next';
import {
  ApprovalType,
  LinkedBill,
  Modification,
  ModificationInput,
} from '@/generated';
import { Formatter } from '@/utils/format';

/** a testing body has to inspect the car with the part (Abnahme) */
export const needsInspection = (type?: ApprovalType) =>
  type === ApprovalType.PARTS_REPORT || type === ApprovalType.SINGLE_APPROVAL;

/** it has a number and maybe conditions */
export const hasApproval = (type?: ApprovalType) =>
  type !== undefined && type !== ApprovalType.NONE;

/**
 * Installed after an Abnahme but not entered in the Zulassungsbescheinigung
 * yet, often done at the next visit of the registration office
 */
export const registrationOpen = (modification: Modification) =>
  modification.installed &&
  needsInspection(modification.approvalType) &&
  !modification.inspectionOpen &&
  !modification.registered;

/** the input that keeps everything as it is */
export const toInput = (modification: Modification): ModificationInput => ({
  part: modification.part,
  manufacturer: modification.manufacturer,
  approvalType: modification.approvalType,
  approvalNumber: modification.approvalNumber,
  conditions: modification.conditions,
  inspectionDate: modification.inspectionDate,
  testingBody: modification.testingBody,
  registered: modification.registered,
  installedDate: modification.installedDate,
  installedOdometer: modification.installedOdometer,
  removedDate: modification.removedDate,
  removedOdometer: modification.removedOdometer,
  notes: modification.notes,
  billIds: modification.bills.map((bill) => bill.id),
});

/** e.g. "Apr 2, 2025 · Mounting · €150.00" */
export const billText = (bill: LinkedBill, format: Formatter, t: TFunction) =>
  [
    format.date(bill.date),
    bill.description || t(`bill.types.${bill.type}`),
    format.money(bill.total),
  ].join(' · ');
