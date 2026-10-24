import { ApprovalType, Modification, ModificationInput } from '@/generated';

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

/** the common ones, any other can be typed */
export const TESTING_BODIES = ['TÜV', 'DEKRA', 'GTÜ', 'KÜS'];

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
  billId: modification.billId,
});
